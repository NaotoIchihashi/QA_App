package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_answer_send.*

class AnswerSendActivity : AppCompatActivity(), View.OnClickListener,DatabaseReference.CompletionListener {

    private lateinit var mQuestion:Question

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_send)

        //QuestionDetailActivityから渡ってきたQuestionのデータを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        //sendButtonにsetOnClickListenerを設定
        sendButton.setOnClickListener(this)
    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if(databaseError == null){
            finish()
        }else{
            Snackbar.make(findViewById(android.R.id.content),"投稿に失敗しました",Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onClick(v: View) {
        //キーボードが出ていたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)

        //DatabaseReference（データのアドレス）を取得
        val databaseReference = FirebaseDatabase.getInstance().reference
        //contens/genre/questionUid/answersの保存先アドレス生成
        val answerRef = databaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)

        val data = HashMap<String,String>()

        //Firebaseからユーザidを取得
        //ユーザidはFirebaseが発行するためFirebaseから取得する必要がある
        data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

        //Preferenceから名前を取得
        //ユーザが投稿の度に表示名を入力しなくて良いように
        //Preferenceにuidと紐づけて表示名を保存している
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY,"")
        data["name"] = name

        //EditTextから回答を取得する
        val answer = answerEditText.text.toString()

        if(answer.isEmpty()){
            //回答が入力されていないときはエラー表示
            Snackbar.make(v,"回答を入力して下さい",Snackbar.LENGTH_LONG).show()
            return
        }
        data["body"] = answer

        progressBar.visibility = View.VISIBLE

        //answerRefの下にdataをpush()メソッドでFirebaseに登録
        //これによりデータの構造としてgenreRef(ジャンル番号)/data(質問情報)/data(回答情報)となる
        answerRef.push().setValue(data,this)


        //お気に入りデータが存在していた場合はお気に入りデータにも回答データを保存し同期をとる
        //お気に入り管理用のアドレス取得
        //お気に入りデータ内の質問データアドレス取得
        val favoriteRef = databaseReference.child(FavoritesPATH).child(mQuestion.uid).child(mQuestion.questionUid)
        //お気に入りデータの取得
        favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            //onDataChange:指定したパス内のデータに対する変更を検知
            //DataSnapshot:呼び出し時点におけるパス内のデータ全体のコピー
            override fun onDataChange(snapshot: DataSnapshot) {
                //dataにDataSnapshotをMap型として取得
                val data = snapshot.value as Map<*, *>?
                //選択したmQuestionUidのお気に入りデータが存在すれば処理実行
                if (data != null) {

                    //ContentsPATHデータとFavoritePATHデータとの同期をとる
                    //まずはanswerRefにpush()したデータをaddListenerForSingleValueEventで取得する
                    answerRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        //onDataChange:指定したパス内のデータに対する変更を検知
                        //DataSnapshot:呼び出し時点におけるパス内のデータ全体のコピー
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val map = snapshot.value as Map<String,String>
//                            Log.d("ABCDE","map:"+map.toString())
                            //念のためmapのnullチェック
                            if(map != null){
                                //ここの繰り返し処理もQuestionDetailActivityと同じ手法で簡略化
                                //snapshotではキーがpush()で生成したアドレス（＝answerUid）で、それに対応して保存したデータが入っている
                                // { key = { name,uid,body } * n という入れ子のデータ構成
                                //QuestionDetailActivityの時よりデータ階層が1つ深いのでデータ読み取り用の一時Map(temp)を用意
                                //なお、Map型であるmapに .keys をつけることでSet型としてkey(answerUid)を取得できる
                                for(key in map.keys){
//                                    Log.d("ABCDE",key)
                                    var data_answer = HashMap<String,String>()
                                    //変数keyにmapのキーを代入し、それをmap[key]と入れ直すことで{ name,uid,body }の子mapを取得できる
                                    val temp = map[key] as Map<String,String>
                                    // ?: は左側がnullなら右側を返すエルビス演算子。左右はメソッドでも良い。
                                    //temp[]の中身が String? なので素直に代入できないため。
                                    //toString()だと中身が万が一 null だった時にエラーになる。
                                    data_answer["body"] = temp["body"] ?: ""
                                    data_answer["name"] = temp["name"] ?: ""
                                    data_answer["uid"] = temp["uid"] ?: ""
                                    val favoriteRef = databaseReference.child(FavoritesPATH).child(mQuestion.uid).child(mQuestion.questionUid).child(AnswersPATH).child(key)

                                  //最初に書いた強引なコード/////////////////////////////////////////////////////////////
//                                  for (i in 0..map.count()-1){
//                                    //mapをList型にキャストすると最初の値が回答データアドレス（Mapのキー）となる
//                                    //キーを指定して入れ子になっているMapデータをmap2として取得
//                                    val map2 = map[map.toList()[i].first] as Map<String,String>
////                                    Log.d("ABCDE","回答データアドレス確認:"+map.toList()[i].first)
//                                    var data_answer = HashMap<String,String>()
//                                    //map2は通常のmapなのでキーを使って素直に値を取得
//                                    data_answer["body"] = map2["body"].toString()
//                                    data_answer["name"] = map2["name"].toString()
//                                    data_answer["uid"] = map2["uid"].toString()
//                                    //FavoritesPATH上のアドレスを取得。回答データアドレスはmap.toList()[i].firstを利用
//                                    val favoriteRef = databaseReference.child(FavoritesPATH).child(mQuestion.uid).child(mQuestion.questionUid).child(AnswersPATH).child(map.toList()[i].first)

                                    favoriteRef.setValue(data_answer)
                                }
                            }
                        }
                        override fun onCancelled(p0: DatabaseError) { }
                    })
               }
            }
            override fun onCancelled(p0: DatabaseError) { }
        })
    }
}
