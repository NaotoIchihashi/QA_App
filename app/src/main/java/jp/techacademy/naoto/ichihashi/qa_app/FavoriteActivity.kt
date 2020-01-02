package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.Toolbar
import android.util.Base64
import android.util.Log
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoriteActivity : AppCompatActivity() {

    //Firebaseのデータベースからデータを読み取るDatabaseReferenceクラスのプロパティ
    private lateinit var mDataBaseReference: DatabaseReference
    //ListViewのプロパティ
    private lateinit var mListView: ListView
    //自作したQuestionクラスを要素とするArrayListのプロパティ
    private lateinit var mQuestionArrayList: ArrayList<Question>
    //自作したQuestionListAdapterアダプタのプロパティ
    private lateinit var mAdapter: FavoriteListAdapter

    //FirebaseのmGenreRefアドレス下のデータの変更を検知するリスナ/////////////////////
    private val mEventListener = object : ChildEventListener {
        //"リスナにとって"データの取得ないし追加があったときに呼び出される
        //既存データについて、設定された直後のリスナにとっては
        //データが追加されているのと同義であり、アドレス直下の子ごとに分けて
        //全ての子データが返される
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            //onChildAddedがDataSnapshotを受け取るので、受け取るための定数を用意
            //DataSnapshotはある時点における登録データ全体のコピー
//            Log.d("ABCDE","X"+dataSnapshot.toString())
            //質問データを取得
            val map = dataSnapshot.value as Map<String, String>
//            Log.d("ABCDE","3"+map.toString())
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val genre = map["genre"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    //画像データはQuestionSendActivityでBase64でencodeして
                    //Firebaseに登録しているのでdecodeして読み取る
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            //回答データを取得
            //Firebaseから受け取ったデータを元にAnswerクラスのオブジェクトを取得するためArrayListを用意
            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                //Answerクラスは質問データに入れ子になっているのでfor文ですべてのデータを取得
                //更にAnswerは複数存在する可能性があるのでFirebaseが発行したkey毎にfor文を実行
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    //データをまとめて自作のAnswerクラスのオブジェクトを取得
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    //ArrayListに登録
                    answerArrayList.add(answer)
                }
            }

            //質問データ、回答データをまとめて自作のQuestionクラスのオブジェクトを取得
            val question = Question(title,body,name,uid,dataSnapshot.key ?: "",genre.toInt(),bytes,answerArrayList)
            //mQuestionArrayListに登録
            mQuestionArrayList.add(question)
            //mAdapterにmQuestionArrayListのデータ更新を知らせる
            mAdapter.notifyDataSetChanged()
        }

        //データに変更があった時に呼び出される
        //第1引数：変更があった場所のDataSnapshot、第2引数：変更があった場所のキー名
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            //DataSnapshotを取得
            val map = dataSnapshot.value as Map<String, String>
            //変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    //このアプリで変更がある可能性があるのはAnswerクラスのみなので
                    //Answerクラスを一度クリアした後に更新する
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {        }
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {        }
        override fun onCancelled(p0: DatabaseError) {        }
    }
    //ここまでChildEventListener///////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        val fToolbar = findViewById<Toolbar>(R.id.fToolbar)
        fToolbar.title = getString(R.string.favorite_intent_button_text)
        setSupportActionBar(fToolbar)

    }

    //QuestionDetailActivityから戻ってきた時にmQuestionArrayListを更新するため
    //onResume()にmQuesitionArrayListの生成処理を記載
    //こうしないとQuestionDetailActivityでお気に入りボタンをONにする度に
    //同じデータが追加表示されていってしまう
    override fun onResume() {
        super.onResume()
        mDataBaseReference = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser
        //ListView取得
        mListView = findViewById(R.id.listView)
        //ListViewの中身を管理するAdapterを取得
        //Adapterを取得
        mAdapter = FavoriteListAdapter(this)
        //onChildAdded,onChildChangedのデータやり取りと
        //AdapterにListView表示に必要なデータを渡すためのQuestionクラスを用意
        mQuestionArrayList = ArrayList<Question>()
        //FavoriteListAdapter内で定義したsetQuestionListメソッドにより
        //mQuestionARrayListを渡す
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        //ListViewにAdapter処理を実行。
        mListView.adapter = mAdapter
        //ユーザのお気に入りデータアドレス(favorite/uid)を取得
        val favoriteRef = mDataBaseReference.child(FavoritesPATH).child(user!!.uid)
        //favoriteRefに対してEventListenerを設定しデータを全て取得する
        favoriteRef!!.addChildEventListener(mEventListener)

        //ListViewのタップを検知するリスナ
        mListView.setOnItemClickListener { parent, view, position, id ->
            //Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }
}