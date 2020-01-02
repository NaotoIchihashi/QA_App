package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Layout
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {

    //Toolbarのプロパティ
    private lateinit var mToolbar: Toolbar
    //ジャンル選択の判別のための定数プロパティ
    private var mGenre = 0

    //Firebaseのデータベースからデータを読み取るDatabaseReferenceクラスのプロパティ
    private lateinit var mDatabaseReference: DatabaseReference
    //ListViewのプロパティ
    private lateinit var mListView: ListView
    //自作したQuestionクラスを要素とするArrayListのプロパティ
    private lateinit var mQuestionArrayList: ArrayList<Question>
    //自作したQuestionListAdapterアダプタのプロパティ
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null


//Firebaseにおいてリスナを設定したmGenreRefアドレス下のデータ変化を検知するリスナ/////////////////////
    private val mEventListener = object : ChildEventListener{
        //"リスナにとって"データ追加があったときに呼び出される
        //既存データについて、設定された直後のリスナにとってはデータが追加されているのと同義であり、
        //アドレス直下の子ごとに分けて全ての子データが第1引数：DataSnapshotととして返される
        //第2引数はリスナを設定したアドレス
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            //onChildAddedがDataSnapshotを受け取るので、受け取るための定数を用意
            //DataSnapshotはある時点における登録データ全体のコピー
            //質問データを取得
            val map = dataSnapshot.value as Map<String,String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val genre = map["genre"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if(imageString.isNotEmpty()) {
                    //画像データはQuestionSendActivityでBase64でencodeして
                    //Firebaseに登録しているのでdecodeして読み取る
                    Base64.decode(imageString, Base64.DEFAULT)
                }else{
                    byteArrayOf()
                }
            //回答データを取得
            //Firebaseから受け取ったデータを元にAnswerクラスのオブジェクトを取得するためArrayListを用意
            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String,String>?
            if (answerMap != null){
                //Answerクラスは質問データに入れ子になっているのでキー毎にfor文ですべてのデータを取得
                for (key in answerMap.keys){
                    val temp = answerMap[key] as Map<String,String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    //データをまとめて自作のAnswerクラスのオブジェクトを取得
                    val answer = Answer(answerBody,answerName,answerUid,key)
                    //answerArrayListに登録
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

        //データ修正があった時に呼び出される
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            //DataSnapshotを取得
            val map = dataSnapshot.value as Map<String,String>
            //変更があったQuestionを探す
            for (question in mQuestionArrayList){
                if(dataSnapshot.key.equals(question.questionUid)){
                    //このアプリで変更がある可能性があるのはAnswerクラスのみなので
                    //Answerクラスを一度クリアした後に更新する
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String,String>?
                    if(answerMap != null){
                        for(key in answerMap.keys){
                            val temp = answerMap[key] as Map<String,String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody,answerName,answerUid,key)
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


//onResumeの設定。お気に入りボタンの切り替えなど他画面での操作結果で状態変化する処理はここに書く
//onCreateは一回しか呼ばれないので状態変化を反映した処理ができない
    override fun onResume() {
        super.onResume()
        //NavigationViewを取得
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        if(mGenre == 0){
            //activity_main.xmlのNavigationView記述のmenuを参照
            //app:menuで指定したres/menu/activity_main_drawerを更に参照
            //activity_main_drawerの最初のItemデータを取得（mGenre = 1）
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }

        //FirebaseAuthからユーザ情報取得。ログインの確認
        val user = FirebaseAuth.getInstance().currentUser
        //favorite_intent_buttonのオブジェクト取得
        //favorite_intent_buttonはnavigationViewのheader(nav_header_main)の中なので
        //navigationView.getHeaderView(0)の指定が必要
        //getHeaderView()の引数はheaderのpositionで、今回は1つしかないので0番
        val favorite_intent_button = navigationView.getHeaderView(0).findViewById<Button>(R.id.favorite_intent_button)
        //ユーザがログインしていればfavorite_intent_buttonをVISIBLE
        if( user == null ){
            favorite_intent_button.visibility = View.GONE
        }else{
            favorite_intent_button.visibility = View.VISIBLE
            //favorite_intent_buttonをタップした時にFavoriteActivityを呼び出す
            favorite_intent_button.setOnClickListener{ v->
                val intent = Intent(applicationContext,FavoriteActivity::class.java)
                startActivity(intent)
            }
        }
    }
//ここまでMainActivityのonResume/////////////////////////////////

//MainActivityのonCreate////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //acttivity_main/app_bar_main/toolbarビューを取得
        mToolbar = findViewById(R.id.toolbar)
        //自作のツールバーを使うのでAndrroidManifest.xmlで事前に
        //MainActivityの設定をデフォルトアクションバーを未使用にしてある
        //　　　　　　　　　　　android:theme="@style/AppTheme.NoActionBar"
        //setSupportActionBar()でmToolbarを有効にする
        setSupportActionBar(mToolbar)

        //fabにFloatingActionButtonの設定///////////////////////////////////
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        //fabにリスナを設定
        fab.setOnClickListener { view ->
            //ジャンルを選択していない場合（mGenre==0)はエラー表示。念のための処理
            if(mGenre == 0){
                Snackbar.make(view,"ジャンルを選択して下さい",Snackbar.LENGTH_LONG).show()
            }else{
            }
            //FirebaseAuthからユーザ情報取得。ログインの確認
            val user = FirebaseAuth.getInstance().currentUser
            //ログインしてなければログイン画面に遷移させる
            if(user == null){
                val intent = Intent(applicationContext,LoginActivity::class.java)
                startActivity(intent)
            }else{
                //ログインしていればmGenreを渡して質問作成画面を起動する
                val intent = Intent(applicationContext,QuestionSendActivity::class.java)
                intent.putExtra("genre",mGenre)
                startActivity(intent)
            }
        }
        //ここまでfabの設定/////////////////////////////////////////////////////

        //ナビゲーションドロワーの設定
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        //Toggle（アプリ画面左上の"三"ボタン)のオブジェクト取得
        //第1引数：drawerを有するアクティビティ、第2引数：DrawerLayout
        //第3引数：Tollbar、第4，5引数：通常使わないのでR.string.app_nameでOK
        val toggle = ActionBarDrawerToggle(this,drawer,mToolbar,R.string.app_name,R.string.app_name)
        //Drawerにリスナ設定
        drawer.addDrawerListener(toggle)
        //DrawerとToggleを同期させる
        toggle.syncState()

        //NavigationViewを取得
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        //NavigationViewにListener設定
        navigationView.setNavigationItemSelectedListener(this)

        //FirebaseのDatabaseReference（Firebase上のデータアドレス管理用のインスタンス）を取得
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        //activity_main(include)app_bar_main(include)content_mainのListView取得
        mListView = findViewById(R.id.listView)
        //ListViewの中身を管理するQuestionsListAdapterを取得
        mAdapter = QuestionsListAdapter(this)
        //onChildAdded,onChildChangedのデータやり取りと
        //AdapterにListView表示に必要なデータを渡すためのQuestionクラスを用意
        mQuestionArrayList = ArrayList<Question>()
        //Adapterにはthisを代入しているので、MainActivityの状態に変化があったときに
        //notifyDataSetChanged()が応答する？
        mAdapter.notifyDataSetChanged()

        //ListViewのタップを検知するリスナ
        mListView.setOnItemClickListener{parent, view, position, id ->
            //タップされた位置のmQuestionArrayListを渡してQuestionDetailActivityを起動する
            val intent = Intent(applicationContext,QuestionDetailActivity::class.java)
                intent.putExtra("question",mQuestionArrayList[position])
                startActivity(intent)
        }
    }
//ここまでMainActivityのonCreate/////////////////////////////////////

    //オプションメニュー menu_mainを生成する
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    //生成したオプションメニュー menu_main.xmlのViewに対する処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if(id == R.id.action_settings){
            //FirebaseAuthからユーザ情報取得。ログインの確認
            val user = FirebaseAuth.getInstance().currentUser
            //ログインしてなければメッセージを出す
            if(user == null){
                Toast.makeText(applicationContext,"ログインしていません", LENGTH_SHORT).show()
            }else{
                //SettingActivityを起動する
                val intent = Intent(applicationContext,SettingActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //NavigationViewのItemがタップされたときの処理/////////////////////////
    //引数はタップされたItemデータ
    override fun onNavigationItemSelected(item:MenuItem):Boolean{
        //引数として受け取ったデータからitemId値を取得
        val id = item.itemId

        //itemIdによってToolbarのtitle表示、mGenreを変更
        if(id == R.id.nav_item1){
            mToolbar.title = getString(R.string.item1_text)
            mGenre = 1
        }else if(id == R.id.nav_item2) {
            mToolbar.title = getString(R.string.item2_text)
            mGenre = 2
        }else if(id == R.id.nav_item3) {
            mToolbar.title = getString(R.string.item3_text)
            mGenre = 3
        }else if(id == R.id.nav_item4) {
            mToolbar.title = getString(R.string.item4_text)
            mGenre = 4
        }

        //DrawerLayoutのViewを取得
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        //Menuのどれかをタップしたら自動的にDrawerLayoutを閉じる（GravityCompat.STARTは左側に閉じる指定）
        drawer.closeDrawer(GravityCompat.START)

        //mGenreに応じてmQuestionArrayListを更新するため一度クリアする
        mQuestionArrayList.clear()
        //QuestionAdapter内のsetQuestionArrayListメソッドを実行して
        //mQuestionArrayListを渡してmAdapterとデータの同期をとる
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        //ListViewに対してAdapter処理を実行し更新する。
        mListView.adapter = mAdapter

        //更新されたmGenreRefのアドレスにリスナを設定し直すため、EventListenerリスナも解除する
        if (mGenreRef != null){
            mGenreRef!!.removeEventListener(mEventListener)
        }

        //DatabaseReferenceによりmGenreRefに応じたFirebase上のアドレスを取得
        //ここで、指定したアドレスが存在しない場合は新規作成される
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        //取得したmGenreRefアドレスに対してmEventListenerを設定
        //addChildEventListenerが設定される時にonChildAdded()が呼び出される
        //onChildAdded()が呼び出された時の処理によりmQuestionArrayListが更新される
        mGenreRef!!.addChildEventListener(mEventListener)

        return true
    }
}

