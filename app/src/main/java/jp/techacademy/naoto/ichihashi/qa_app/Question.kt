package jp.techacademy.naoto.ichihashi.qa_app

import java.io.Serializable
import java.util.ArrayList

//Firebaseから取得した質問に関するデータを保持するためのクラス
//Intentで渡せるようにSerializableクラスを実装　→　これだけでIntentでデータを渡せるようになる
//uidおよびquestionUidはユーザおよびその投稿に対してFirebaseが発行するキーを格納するためのもの
//また、Mapで入れ子構造にし安くなり、データにアクセスするのも変数に ,title　をつけるだけでよくお手軽になる
class Question(val title: String, val body: String, val name: String, val uid: String,
               val questionUid: String, val genre: Int, bytes: ByteArray, val answers:ArrayList<Answer>):Serializable {
    //質問には画像投稿が可能なので画像データを格納するためByteArrayバイト配列型の定数を用意
    //バイト配列、バイナリ配列とは文字や画像など様々な任意のデータ型を
    //単なるビットパターンとして汎用的に処理する場合に使用するデータ型
    val imageBytes: ByteArray

    init{
        imageBytes = bytes.clone()
    }
}