package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Context
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_setting.*
import java.lang.ref.PhantomReference

class SettingActivity : AppCompatActivity() {

    private lateinit var mDataBaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        //Preferenceオブジェクトを取得
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        //PreferenceからNameKEYキーに紐づけられた表示名を取得
        //なければ空文字を取得
        val name = sp.getString(NameKEY,"")
        //nameText2ビューに取得した表示名を設定
        nameText2.setText(name)

        mDataBaseReference = FirebaseDatabase.getInstance().reference

        //ツールバーのタイトル用文字列をres/values/stringから取得
        title = getString(R.string.menu_text)

        //表示名変更ボタンを押したときの処理
        changeButton.setOnClickListener{v ->
            //INPUT_METHOD_SERVICE（キーボード）のインスタンス取得
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //INPUT_METHOD_SERVICEを非表示にする
            im.hideSoftInputFromWindow(v.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)

            //ログイン済みのユーザを取得する
            val user = FirebaseAuth.getInstance().currentUser

            //userがnull、ログインしていない場合の処理
            if(user == null){
                Snackbar.make(v,"ログインしていません",Snackbar.LENGTH_LONG).show()
            }else{
                //変更した表示名をFirebaseに保存する
                val name = nameText2.text.toString()
                //user情報が登録されているアドレスを取得
                val userRef = mDataBaseReference.child(UsersPATH).child(user.uid)
                val data = HashMap<String,String>()
                //キー"name"をつけたMapでFirebaseのデータを更新
                data["name"] = name
                userRef.setValue(data)

                //変更した表示名をPreferenceにも保存する
                val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = sp.edit()
                editor.putString(NameKEY,name)
                editor.commit()

                Snackbar.make(v,"表示名を変更しました",Snackbar.LENGTH_LONG).show()
            }
        }

        //ログアウトボタンを押した時の処理
        logoutButton.setOnClickListener{v ->
            //FirebaseAuthのsignout()メソッド実行
            FirebaseAuth.getInstance().signOut()
            //表示名を空文字にする
            nameText2.setText("")
            Snackbar.make(v,"ログアウトしました",Snackbar.LENGTH_LONG).show()
            //ログアウト時にボタン操作を不可にする
            logoutButton.isEnabled = false
            //ボタンの色を変更する。argbは(透明度、R,G,B)で指定
            logoutButton.setBackgroundColor(Color.argb(50,255,98,0))
            changeButton.isEnabled = false
            changeButton.setBackgroundColor(Color.argb(50,255,98,0))
        }

    }
}
