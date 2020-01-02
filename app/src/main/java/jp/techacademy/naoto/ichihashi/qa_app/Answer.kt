package jp.techacademy.naoto.ichihashi.qa_app

import java.io.Serializable

//Firebaseから取得した回答に関するデータを保持するクラス
//Intentで渡せるようにSerializableクラスを実装する　→　これだけでIntentでデータを渡せるようになる
//uidおよびanswerUidはユーザおよびユーザの投稿に対してFirebaseが発行するキーを格納するためのもの
class Answer(val body: String, val name: String, val uid: String, val answerUid: String):Serializable