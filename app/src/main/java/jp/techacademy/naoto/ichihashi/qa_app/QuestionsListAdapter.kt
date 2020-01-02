package jp.techacademy.naoto.ichihashi.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class QuestionsListAdapter(context: Context):BaseAdapter() {
    //LayoutInflaterクラスのプロパティ
    //LayoutInflaterによりAdapter起動元Activityの
    //setContentView()以外のLayoutファイルを使用できる
    private var mLayoutInflater: LayoutInflater
    //ListViewに表示するArrayListを用意
    private var mQuestionArrayList = ArrayList<Question>()

    //LayoutInflaterのオブジェクト取得
    init{
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    //ArrayListのデータ数取得
    override fun getCount(): Int {
        return mQuestionArrayList.size
    }

    //タップされたら、そのItemIdのデータを返す
    override fun getItem(position: Int): Any {
        return mQuestionArrayList[position]
    }

    //タップされたら、その場所のItemIdを返す
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    //Viewを生成するメソッド
    //第1引数：表示するItem位置、第2引数：前回のgetView呼び出し時に表じれていたView
    //第3引数：getViewで生成したViewの親
    //getViewが呼び出された初回は第2引数は必ずnullになる
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        //convertViewを取得
        var convertView = convertView
        //初回呼び出し時はnullになるのでInflate処理
        if(convertView == null){
            //第1引数：LayoutデータのID、第2引数：Layoutの適用先
            //第3引数：Layoutを適用先にアタッチするか
            convertView = mLayoutInflater.inflate(R.layout.list_questions,parent,false)
        }

        //各Viewに設定するデータをmQuestionArrayListから取得
        //mQuestionArrayListはQuestionクラスのリストであり、以下のネスト構造になっている
        // NO = { title , body ,name ,uid ,questionUid ,genre imageBytes , answers }
        //                                                             　※answersは更に NO = { body , name , uid , answerUid }　となっている
        //このデータ構造を考慮して適した読み書き手法を取る必要がある
        //ここではgetViewの引数である position: Int を利用して
        //mQuestionArrayList[position]　とした上で .title　をつけてデータにアクセスしている
        //なお、answersはもう1つ階層が深いのでデータアクセス用のMap型変数を一時的に用意するなどして対応する
        val titleText = convertView!!.findViewById<View>(R.id.titleTextView) as TextView
        titleText.text = mQuestionArrayList[position].title
//        Log.d("ABCDE","5"+mQuestionArrayList[position].toString())

        val nameText = convertView.findViewById<View>(R.id.nameTextView) as TextView
        nameText.text = mQuestionArrayList[position].name

        val resText = convertView.findViewById<View>(R.id.resTextView) as TextView
        val resNum  = mQuestionArrayList[position].answers.size
        resText.text = resNum.toString()

        val bytes = mQuestionArrayList[position].imageBytes
        if(bytes.isNotEmpty()){
            val image = BitmapFactory.decodeByteArray(bytes,0,bytes.size).copy(Bitmap.Config.ARGB_8888,true)
            val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
            imageView.setImageBitmap(image)
        }

        return convertView
    }

    fun setQuestionArrayList(questionArrayList:ArrayList<Question>){
        mQuestionArrayList = questionArrayList
//        Log.d("ABCDE","1"+mQuestionArrayList.toString())
    }
}