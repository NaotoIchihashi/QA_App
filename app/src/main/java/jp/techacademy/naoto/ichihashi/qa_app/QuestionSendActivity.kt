package jp.techacademy.naoto.ichihashi.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream
import java.lang.Exception

class QuestionSendActivity : AppCompatActivity(), View.OnClickListener,DatabaseReference.CompletionListener {
    //permission、Intentのための定数を定義
    //companion objectはSingletonを生成する。
    //Singletonとはオブジェクトを1つしか作れない。複数作れてしまうと困る場合に使う。
    companion object{
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    //ジャンルNoを保持するプロパティ、カメラ撮影した画像保存先URIを保持するプロパティ
    private var mGenre:Int = 0
    private var mPictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        //Intentで渡ってきたジャンル番号を保持する
        val extras = intent.extras
        mGenre = extras.getInt("genre")

        //ツールバーのタイトル用文字列をres/values/stringから取得
        title = getString(R.string.questionSendActivity_title)

        sendButton.setOnClickListener(this)
        imageView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        //タップされたViewがimageViewの場合の処理
        if(v == imageView){
            //Android6.0以降の場合の処理
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                //permissionの許可されているか確認
                //Manifestについてimportするのは" android.Manifest "
                if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    showChooser()
                }else{
                    //許可されていない場合は許可ダイアログ表示
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),PERMISSIONS_REQUEST_CODE)
                    return
                }
            }else{
                showChooser()
            }
        //タップされたViewがsendButtonの場合の処理
        }else if(v == sendButton) {
            //キーボードが出てたら閉じる
            //SERVICEからInputMethodManagerオブジェクト取得
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //hideSoftInputFromWindowメソッドを呼び出してキーボードを閉じる
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            //FirebaseDatabase.getInstance().referenceでこのアプリが持つFirebase上の最上位アドレスを取得
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            //最上位アドレス/contents/mGenreのネスト構造としたFirebase上のアドレスを取得(生成)
            //このアドレスの作り方によってデータ構造を決定できる
            val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())
            //HashMapオブジェクトを用意
            val data = HashMap<String, String>()

            //Firebaseからログインしているユーザ(currentUser)のID(uid)を取得
            data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

            //各EditTextからタイトルと本文を取得する
            val title = titleText.text.toString()
            val body = bodyText.text.toString()

            //タイトルが入力されていないときはエラー表示
            if (title.isEmpty()) {
                Snackbar.make(v, "タイトルを入力して下さい", Snackbar.LENGTH_LONG).show()
                return
            }

            //質問が入力されていないときはエラー表示
            if (body.isEmpty()) {
                Snackbar.make(v, "質問を入力して下さい", Snackbar.LENGTH_LONG).show()
                return
            }

            //Preferencesオブジェクトを取得
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            //キー:NameKEYに格納されているStringデータを取得。
            //第2引数はデータが存在しなかった時のデフォルト値。
            val name = sp.getString(NameKEY, "")

            //各EditTextの文字列を取得
            data["title"] = title
            data["body"] = body
            data["name"] = name
            data["genre"] = mGenre.toString()

            //imageViewビューから.drawableで画像をBitmapDrawableクラスとして取得
            //BitmapDrawableクラスはBitmapクラスを含みつつ画像の伸縮など機能が豊富
            //後に画像を加工するのでBitmapDrawableクラスで画像を取得
            val drawable = imageView.drawable as? BitmapDrawable

            //添付画像が設定されていれば画像を取り出してBASE64エンコードする
            if (drawable != null) {
                //BitmapDrawableからBitmapクラスのオブジェクトを取得
                val bitmap = drawable.bitmap
                //ByteArrayOutputStreamクラスのオブジェクトを取得
                //ByteArrayクラスに変換して出力する
                val baos = ByteArrayOutputStream()
                //bitmapオブジェクトをJPEGフォーマット、
                //品質80(100が最大)に指定してOutputStreamオブジェクトに渡す
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                //ByteArrayクラスに変換されたデータ（元々imageViewの画像）を
                //Base64のDEFAULTで文字列にencodeして取得
                val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                //文字列に変換された画像データをdataにキー:imageと紐づけてdataに取得
                data["image"] = bitmapString
            }

            //genreRefごとにの下にdataをpush()メソッドでFirebaseに登録
            //これによりデータの構造としてgenreRef(ジャンル番号)/data(質問情報)となる
            //val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())
            genreRef.push().setValue(data, this)

            progressBar.visibility = View.VISIBLE

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<String>,grantResults:IntArray) {
        when(requestCode){
            PERMISSIONS_REQUEST_CODE -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    showChooser()
                }
                return
            }
        }
    }

    private fun showChooser(){
        //Intentオブジェクトを取得
        //ACTION_GET_CONTENTで端末内部のデータを選択、取得できる（暗黙的Intent）
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        //外部から取得するデータをimageファイルに限定
        galleryIntent.type = "image/*"
        //暗黙的Intentの受け手をカテゴリで限定。
        //CATEGORY_OPENABLEは開くことができるデータに限定。
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        //カメラで撮影するIntent
        //System.currentTimeMillis()メソッドで現在時刻を取得し
        //Stringに変換して.jpgを追加してファイル名とする
        val filename = System.currentTimeMillis().toString() + ".jpg"
        //ContentValuesクラスのオブジェクトを取得
        //このクラスはキーと値を保持することができ、putメソッドでデータ追加できる
        val values = ContentValues()
        //MediaStoreはAndroid標準に備わっている画像、音楽、動画の情報を収集する機能
        //更にMediaStoreはContentProviderを利用したデータベース機能を有する
        //ContentProviderはSQLiteに格納されているデータを他アプリと共有する中核機能
        //SQLiteはAndroid標準に備わっているデータベース機能
        //ContentResolverの使用によってデータの保存・登録が可能
        //valuesにTITLEとしてfilenameを設定
        values.put(MediaStore.Images.Media.TITLE,filename)
        //valuesにMIME_TYPE（メディアタイプ）としてjpegを設定
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
        //contentResolver.insert()メソッドを呼び出してデータ保存領域を確保
        //データ保存先のURIを返しmPictureUriに取得
        //EXTERNAL_CONTENT_URIが外部メディアのURIを指す
        mPictureUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)


        //IntentにACTION_IMAGE_CAPTUREを追記すると
        //カメラアプリを起動して画像を取得できる
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //MediaStore.EXTRA_OUTPUT：撮影画像を出力する設定
        //第2引数は出力先Uriを指定する必要があり、Uriクラスを事前に用意しておく必要がある
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,mPictureUri)

        //ギャラリー選択のIntentを与えてcreatechooserメソッドを呼ぶ
        val chooserIntent = Intent.createChooser(galleryIntent,"画像を取得")
        //chooserIntentにカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        //起動したアクティビティから何らかのデータを受け取る場合は
        //startActivityForResultでIntentを起動する。
        //第2引数は返ってくる結果を識別するためのコード（Int）
        //これでIntentを起動するとonActivityResultをoverrideした結果処理が必要になる。
        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)

    }

    //startActivityForResultを結果処理のためのメソッド
    //受け取る引数は３つ
    //requestCode：startActivityForResultの第2引数
    //resultCode：要求したアクティビティ操作が成功したかどうかの識別。RESULT_OK　か　RESULT_CANCELEDのいずれか。
    //data:アクティビティの結果データ（カメラの場合は画像ファイル）
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //requestCodeとstartActivityForResultの第2引数（識別コード）が一致しているか
        if(requestCode == CHOOSER_REQUEST_CODE){
            //アクティビティ操作が失敗した場合の処理。
            //想定外のデータを受け取っている場合もあるのでアクティビティ実行前の状態に初期化する。
            if(resultCode != Activity.RESULT_OK){
                //画像データ格納用にmPictureUriを渡してIntent起動したので中身が入っているか確認
                if(mPictureUri != null){
                    //contentResolverに確保したデータ保存領域を削除
                    contentResolver.delete(mPictureUri!!,null,null)
                    //mPictureUriを初期化
                    mPictureUri = null
                }
                return
            }

            //Intentの結果データ(data)がnullの場合（ユーザがカメラを使用した場合）
            //カメラの画像データ保存先であるmPitureUriを取得
            //ユーザが端末内部データから取得した場合はすでにdataが取得されているのでそのまま使う
            val uri = if(data == null || data.data == null) mPictureUri else data.data

            //Bitmapとして画像を取得する定数を用意
            val image: Bitmap
            try{
                //contentResolverオブジェクトを取得
                val contentResolver = contentResolver
                //contentResolver.openInputStreamメソッドでuri指定先のデータを読み込んで開く
                //ここではstreamクラスとして読み込んでいる
                //steamクラスはSerializeされた抽象クラスで、
                //あらゆる形式のデータ入出力を画一的に扱える
                val inputStream = contentResolver.openInputStream(uri!!)
                //BitmapFactory.decodeStreamでinputStreamをBitmapに変換
                image = BitmapFactory.decodeStream(inputStream)
                //inputStreamで開いたデータを閉じる。閉じないとデータが開きっぱなしでメモリが永遠に残る。
                inputStream!!.close()
            //画像取得処理中に例外が発生したら何もしない
            }catch(e:Exception){
                return
            }

            //取得したBitmapのwidth、heightを取得
            val imageWidth = image.width
            val imageHeight = image.height
            //長辺500ピクセルに変換するための倍率を取得
            //Math.miniは引数の中の最小値を取得する
            val scale = Math.min(500.toFloat() / imageWidth,500.toFloat() / imageHeight) // (1)
            //Matrixクラスのオブジェクトを取得
            //Matrixクラスは座標変換処理機能を持つクラス。画像加工などでよく使う。
            val matrix = Matrix()
            //postScaleメソッドで変換処理を設定
            matrix.postScale(scale,scale)
            //createBitmapメソッドで画像を変換
            //第1引数は元画像、第2,3,4,5引数は元の画像サイズ
            //第6引数は処理内容が設定されたMatrixクラス
            //第7引数はfilter処理の有無。trueにするとアンチエイリアスがかかる。
            val resizedImage = Bitmap.createBitmap(image,0,0,imageWidth,imageHeight,matrix,true)

            //BitmapをImageViewに設定する
            imageView.setImageBitmap(resizedImage)
            //画像処理が終了したのでmPictureUriは初期化しておく
            mPictureUri = null
        }
    }

    //Firebaseに対する何からの操作（このクラスではpush()メソッド）が完了したら呼び出される
    //第1引数：DatabaseErrorでエラーメッセージ。成功した場合はnull。
    //第2引数：操作した場所のリファレンス（アドレス）
    override fun onComplete(databaseError:DatabaseError?,databaseReference:DatabaseReference) {
        progressBar.visibility = View.GONE
        if(databaseError == null){
            finish()
        }else{
            Snackbar.make(findViewById(android.R.id.content),"投稿に失敗しました",Snackbar.LENGTH_LONG).show()
        }
    }
}
