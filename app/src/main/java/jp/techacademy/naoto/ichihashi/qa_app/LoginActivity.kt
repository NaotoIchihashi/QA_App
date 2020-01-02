package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*
import java.lang.ref.Reference

class LoginActivity : AppCompatActivity() {
    //ユーザの認証情報管理の機能を持つFirebaseAuthのプロパティ
    private lateinit var mAuth: FirebaseAuth
    //処理の完了を受け取るリスナのプロパティ
    private lateinit var mCreateAccountListener : OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    //データベースの読み書きに必要なDatabaseReferenceクラスのプロパティ
    private lateinit var mDataBaseReference: DatabaseReference

    //アカウント作成処理か否かを判別するフラグ
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //FirebaseDatabaseのインスタンス取得
        mDataBaseReference = FirebaseDatabase.getInstance().reference
        //FirebaseAuthのインスタンス取得
        mAuth = FirebaseAuth.getInstance()

        //アカウント作成処理完了のリスナー設定。引数として処理の成否が渡ってくる
        mCreateAccountListener = OnCompleteListener { task ->
            //isSuccessfulメソッドで成功したかどうか判定
            if(task.isSuccessful){
                //成功した場合、ログインを行う
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                //loginメソッドを呼び出す
                login(email,password)
            }else{
                //失敗した場合、エラー表示
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view,"アカウント作成に失敗しました",Snackbar.LENGTH_LONG).show()
                //プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        //ログイン処理完了のリスナー/////////////////////////////
        mLoginListener = OnCompleteListener { task ->
            if(task.isSuccessful){
                //成功した場合
                //ユーザ情報を取得
                val user = mAuth.currentUser
                //FirebaseDatabaseからUserPATH下のuidデータを取得
                //childメソッドでキー：UserPATHキーと値：uidを格納
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)

                //アカウント作成直後の場合の処理
                if(mIsCreateAccount){
                    //アカウント作成の時はEditTextのテキストを取得してFirebaseに保存する
                    val name = nameText.text.toString()
                    //Firebaseがキーと値の組み合わせて保存するのでデータやり取り用にMapオブジェクトを用意
                    val data = HashMap<String,String>()
                    //nameキーにname(nameTextから取得したString文字列)を取得
                    data["name"] = name
                    //Firebaseのユーザ情報アドレスuserRefのdataを更新
                    userRef.setValue(data)
                    //表示名をPreferenceに保存
                    saveName(name)
                }else{
                    //userRefにデータ更新を検知するためのリスナを登録
                    userRef.addListenerForSingleValueEvent(object :ValueEventListener{
                        //onDataChange:指定したパス内のデータに対する変更を検知
                        //DataSnapshot:呼び出し時点におけるパス内のデータ全体のコピー
                        override fun onDataChange(snapshot: DataSnapshot) {
                            //dataにDataSnapshotをMap型として取得
                            val data = snapshot.value as Map<*,*>?
                            //dataからnameを取得しPreferenceに保存
                            saveName(data!!["name"] as String)
                        }
                        override fun onCancelled(p0: DatabaseError) { }
                    })
                }
                //プログレスバーを非表示にする
                progressBar.visibility = View.GONE
                //Activity
                finish()
            }else{
                //失敗した場合、エラー表示
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view,"ログインに失敗しました",Snackbar.LENGTH_LONG).show()
                //プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }
        //ここまでmLoginListenerの処理//////////////////////////////////

        //ツールバーのタイトル用文字列をres/values/stringから取得
        title = getString(R.string.login_button_text)

        //アカウント作成ボタン(createButton)をタップした時の処理
        createButton.setOnClickListener{v ->
            //キーボードが出てたら閉じる
            //SERVICEからInputMethodManagerオブジェクト取得
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //hideSoftInputFromWindowメソッドを呼び出してキーボードを閉じる
            im.hideSoftInputFromWindow(v.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)

            //各EditTextの文字列を取得
            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            //email,password,nameがきちんと入力されていたら
            if(email.length != 0 && password.length >= 6 && name.length != 0){
                //フラグを立ててcreateAccountメソッドを呼び出す
                mIsCreateAccount = true
                createAccount(email,password)
            }else{
                //エラー表示
                Snackbar.make(v,"正しく入力して下さい",Snackbar.LENGTH_LONG).show()
            }
        }

        //ログインボタン(loginButton)をタップした時の処理
        loginButton.setOnClickListener{ v ->
            //キーボードが出てたら閉じる
            //SERVICEからInputMethodManagerオブジェクト取得
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //hideSoftInputFromWindowメソッドを呼び出してキーボードを閉じる
            im.hideSoftInputFromWindow(v.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)

            //各EditTextの文字列を取得
            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            //各文字列がきちんと入力されていたら
            if(email.length != 0 && password.length >= 6){
                //フラグを落とす
                mIsCreateAccount = false
                //loginメソッドを呼び出す
                login(email,password)
            }else{
                //エラー表示
                Snackbar.make(v,"正しく入力して下さい",Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun createAccount(email:String,password:String){
        //プログレスバーを表示する
        progressBar.visibility = View.VISIBLE
        //createUserWithEmailAndPasswordメソッドでemailとpasswordを引数として
        //Firebaseに渡してアカウント作成処理を依頼
        //同時にアカウント作成処理完了リスナを設定
        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(mCreateAccountListener)
    }

    private fun login(email: String,password: String){
        //プログレスバーを表示する
        progressBar.visibility = View.VISIBLE
        //signInWithEmailAndPasswordメソッドでemailとpasswordを引数として
        //Firebaseに渡してログイン処理を依頼
        //同時にログイン処理完了リスナを設定
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(mLoginListener)
    }

    private fun saveName(name:String){
        //Preferencesオブジェクトを取得
        //Preferencesは1キー:1データでアプリ情報を読み書きできるAndroidの機能
        //前回起動時の設定などを保存しておくのに便利
        //大量のデータ保存には向かない
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        //書き込み処理のためeditorオブジェクトを取得
        val editor = sp.edit()
        //NameKEYにnameを登録
        editor.putString(NameKEY,name)
        //commitして保存を反映
        editor.commit()
    }
}
