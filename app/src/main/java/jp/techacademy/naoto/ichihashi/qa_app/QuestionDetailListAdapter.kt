package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

//QuestionDetailActivityからmQuestionが渡される
class QuestionDetailListAdapter(context: Context,private val mQuestion: Question):BaseAdapter() {
    //list_question_detail.xmlとlist_answer.xmlを識別する定数
    companion object{
        private val TYPE_QUESTION =0
        private val TYPE_ANSWER = 1
    }

    private var mLayoutInflater:LayoutInflater? = null

    init{
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return 1 + mQuestion.answers.size
    }

    //タップされたポジションIdをTYPE_QUESTIONかTYPE_ANSWERかに識別
    //本アプリでは1行目が質問(TYPE_QUESITON)なので position==0 or else で識別
    override fun getItemViewType(position: Int): Int {
        return if(position == 0){
            TYPE_QUESTION
        }else{
            TYPE_ANSWER
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Any {
        return mQuestion
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    //positionによって使用するxmlを分ける
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

//質問欄の作成/////////////////////////////////////////
        if(getItemViewType(position) == TYPE_QUESTION){
            if (convertView == null){
                convertView = mLayoutInflater!!.inflate(R.layout.list_question_detail,parent,false)!!
            }

            val body = mQuestion.body
            val name = mQuestion.name

            val bodyTextView = convertView.findViewById<View>(R.id.bodyTextView) as TextView
            bodyTextView.text = body

            val nameTextView = convertView.findViewById<View>(R.id.nameTextView) as TextView
            nameTextView.text = name

            val bytes = mQuestion.imageBytes
            if(bytes.isNotEmpty()){
                val image = BitmapFactory.decodeByteArray(bytes,0,bytes.size).copy(Bitmap.Config.ARGB_8888,true)
                val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
                imageView.setImageBitmap(image)
            }

            //FirebaseAuthからユーザ情報を取得
            val user = FirebaseAuth.getInstance().currentUser
            //Firebaseのreference取得用のオブジェクト取得
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            //お気に入り管理用のアドレス用意
            //FavoritesPATHデータはユーザ毎に管理するので2番目のアドレスはmQuestion.uid(選択した質問の投稿者)でなく
            //今ログインしているユーザにすることに注意
            val favoriteRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid)
            //favoritebuttonのオブジェクト取得
            val favorite_button = convertView.findViewById<Switch>(R.id.favorite_button)
            //お気に入りボタンの表示設定。ユーザがログインしていればfavorite_buttonをVISIBLE//////////////////////////
            if( user == null ){
                favorite_button.visibility = View.GONE
            }else {
                //お気に入りデータがすでに存在すればtrueにする処理/////////////////
                //favoriteRef下に保存されているデータを1回のみ取得
                favoriteRef.addListenerForSingleValueEvent(object :ValueEventListener{
                    //onDataChange:指定したパス内のデータを子ごとに取得
                    //DataSnapshot:onDataChange呼び出し時点におけるパス内のデータ全体のコピー
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //dataにDataSnapshotをMap型として取得
                        val data = snapshot.value as Map<*, *>?
                        //お気に入りデータが存在すればtrue
                        if (data != null) {
                            favorite_button.isChecked = true
                        }
                    }
                        override fun onCancelled(p0: DatabaseError) { }
                    })
                //favorite_button表示
                favorite_button.visibility = View.VISIBLE
            }
            //ここまでお気に入りボタンの表示設定///////////////////////////////////////////////////////////////////////

            //favorite_buttonのリスナ設定
            favorite_button.setOnCheckedChangeListener{v,isChecked ->
                //favorite_buttonがtrueになった時の処理
                if(isChecked){
                    //ContentsPATH(ジャンル別データ)と同期したFavoritesPATH(お気に入りデータ)を保存する
                    //FavoritesPATHでユーザ毎に情報を保存するため
                    //FavoritesPATH/uid/questionUidのネスト構造としたアドレスを生成してsetValue()
                    //アドレスを完全に指定するのでpush()はしない
                    //FavoriteActivityで取り出し易くするためデータは丸ごとコピーする
                    val data = HashMap<String,String>()
                    data["uid"] = user!!.uid
                    data["title"] = mQuestion.title
                    data["body"] = body
                    data["name"] = name
                    data["genre"] = mQuestion.genre.toString()

                    //添付画像が設定されている場合
                    //MainActivityのonChildAddedで受け取る際にBase64でdecodeしているのでencodeToStringし直す
                    val bitmapString = Base64.encodeToString(mQuestion.imageBytes, Base64.DEFAULT)
                    if (bitmapString != "") {
                        //文字列に変換された画像データをdataにキー:imageと紐づけてdataに取得
                        data["image"] = bitmapString
                    }
                    //質問データ投稿時にすでに質問データIDが発行されているので、IDが自動発行されるpush()ではなく
                    //setValue()でアドレスを直接指定してFirebaseにデータを登録する。
                    //これによりContentsPATH下のquestionUidとFavoritesPATH下のquestionUidを一致させることができる
                    favoriteRef.setValue(data)

                    //回答データもお気に入りデータに保存する
                    //mQuestionから回答情報を取得
                    var data_answer = HashMap<String,String>()
//                    Log.d("ABC",mQuestion.answers.toString())
                    //回答データが入っていれば処理を実行
                    if (mQuestion.answers != null){
                        //QuestionDetailActivityと同じ手法の繰り返し処理によるデータ取得
                        for (answer in mQuestion.answers){
                            data_answer["body"] = answer.body
                            data_answer["name"] = answer.name
                            data_answer["uid"] = answer.uid
//                            Log.d("ABC",answer.answerUid)
                            //mQuestionにはContentsPATH化のanswersのアドレス情報が入っている
                            //それを参照したアドレスとすることでanswersの保存先アドレスもContentsPATH化のデータと同じにできる
                            val favoriteRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid).child(AnswersPATH).child(answer.answerUid)

//                      for文を数字ベースの繰り返し処理でやろうとした初期のコード
//                      val answerMap = mQuestion.answers
//                       for (i in 0..answerMap.count()-1){
//                          data_answer["body"] = answerMap[i].body
//                          data_answer["name"] = answerMap[i].name
//                          data_answer["uid"] = answerMap[i].uid
//                          mQuestionにはContentsPATH化のanswersのアドレス情報が入っている
//                          それを参照したアドレスとすることでanswersの保存先アドレスもContentsPATH化のデータと同じにできる
//                          val favoriteRef = dataBaseReference.child(FavoritesPATH).child(mQuestion.uid).child(mQuestion.questionUid).child(AnswersPATH).child(answerMap[i].answerUid)

                            favoriteRef.setValue(data_answer)
                        }
                    }
                }else {
                    //isCheckedがnullの場合はお気に入りデータを削除
                    favoriteRef.setValue(null)
                }
            }

//ここまで質問欄の作成//////////////////////////////////////////////

//回答欄の作成/////////////////////////////////////////////////////
        }else{
            if(convertView == null){
                convertView = mLayoutInflater!!.inflate(R.layout.list_answer,parent,false)!!
            }

            val answer = mQuestion.answers[position - 1]
            val body = answer.body
            val name = answer.name

            val bodyTextView = convertView.findViewById<View>(R.id.bodyTextView) as TextView
            bodyTextView.text = body

            val nameTextView = convertView.findViewById<View>(R.id.nameTextView) as TextView
            nameTextView.text = name
        }

        return convertView

    }
}