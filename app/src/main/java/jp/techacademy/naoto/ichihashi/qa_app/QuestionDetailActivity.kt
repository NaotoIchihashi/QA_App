package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Switch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef:DatabaseReference


    //mAnswerRef(回答情報)に対するリスナのためのEventListener/////////////////////////////////
    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            //FirebaseからdataSnapshotにてデータをまとめてmapに受け取る
            val map = dataSnapshot.value as Map<String, String>
            //dataSnapshotからUidを取得
            //リスナを設定したアドレスの階層によってSnapShotの構造が異なるので
            //データ構造、型に合ったデータの取り出し方をすること
            val answerUid = dataSnapshot.key ?: ""

            //mQuestionに保存されている既存のanswersと照合
            //answersは自作のAnswerクラスであり、中身はbody,name,uid,answerUid
            //for文の繰り返し処理は数値だけでなく文字や自作クラスでも可能
            //この処理では任意の変数:answerに対してmQuestion.answersの中身を
            //1個ずつ取り出して代入する繰り返す処理となる
            for (answer in mQuestion.answers) {
                //answerに対してanswerArrayListが1個ずつ代入されるので
                //answerArrayListの中からanswerUidを取り出すには .answer を後ろに付ければ良い
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            //新規のUidが存在する場合は各々のデータを取得
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            //mQuestionに追加する新しいAnswerクラスを生成
            val answer = Answer(body, name, uid, answerUid)
            //mQuestionに新しいAnswerクラスを追加
            //Questionクラスは質問情報を持ち、answersの中に更に回答情報が入っている入れ子構造
            mQuestion.answers.add(answer)
            //情報の更新をmAdapter(QuestionDetailListAdapter)に知らせる
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {        }
        override fun onChildRemoved(p0: DataSnapshot) {        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {        }
        override fun onCancelled(p0: DatabaseError) {        }
    }

    //ここまでがFirebaseのEventListener////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        //MainActivityから渡ってきたQuestionのオブジェクトを取得
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        //mQuestionからtitleを取得してアクションバーのタイトルに設定
        title = mQuestion.title

        //QuestionDetailListAdapterにmQuestionを渡す
        mAdapter = QuestionDetailListAdapter(this,mQuestion)
        //listViewにadapter処理を実行してリストビュー生成
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        //fabの設定//////////////////////////////
        fab.setOnClickListener{
            //ログイン済のユーザ情報を取得
            val user = FirebaseAuth.getInstance().currentUser

            if(user == null){
                //ユーザがログインしてなければログイン画面に遷移させる
                val intent = Intent(applicationContext,LoginActivity::class.java)
                startActivity(intent)
            }else{
                //ユーザがログイン状態であればAnswerSendActivityを呼び出す
                val intent = Intent(applicationContext,AnswerSendActivity::class.java)
                //呼び出す際にmQuestionデータを渡す
                intent.putExtra("question",mQuestion)
                startActivity(intent)
            }
        }

        //AnswerSendActivityからユーザが戻ってきた時に回答一覧画面を更新する処理
        //FirebaseDatabaseのreference（データベースのアドレス）を取得
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        //AnswersPATH(回答データの保存先)のreference（アドレス）を取得
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        //取得したreference(mAnswerRef,回答情報)に対する変更をリスナするeEventListenerを設定
        mAnswerRef.addChildEventListener(mEventListener)
    }

}
