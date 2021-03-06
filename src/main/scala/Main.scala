package nascala

/*
 通常WebサーバーはJ2EE コンテナ上にデプロイするようにしますが、
 ここでは構成のシンプルさにこだわって、
 コンソールアプリケーションとしてWebサーバーを起動しています
 
 J2EE コンテナ上で実行する場合は、下記URLのsbtのWeb Pluginを使用するとよいです
 https://github.com/JamesEarlDouglas/xsbt-web-plugin
*/
object Main extends App {
  unfiltered.jetty.Http.anylocal.plan(Server).run ( s =>
    unfiltered.util.Browser.open(s"http://127.0.0.1:${s.port}/")
  )
}

// 単語とその出現回数。
// この型の値を作って、HTMLに変換して返すのがこのアプリの大まかな流れ
// 命名ミス。 case class Occrrence(word: String, time: Int) の方がよかった
case class Word(name: String, occurrence: Int)

object Server extends unfiltered.filter.Plan{
  import unfiltered.request._
  import unfiltered.response._

  def intent = {
    case Path(Seg("resources":: xs)) => ResponseResource(xs.mkString("/"))
    case Params(User(user)) =>
      Html5(
        HatenaDiary.page(user)
          |> Analyzer.frequentWords(10)
          |> Template.frequentWordPage(user)
      )
    case _ => Html5(Template.mainPage)
  }

  object User extends Params.Extract("user", Params.first)

  case class ResponseResource(filename: String) extends ResponseStreamer{
    def stream(os: java.io.OutputStream) {
      // src/main/resources/ のファイルを取得
      val is = getClass.getClassLoader.getResourceAsStream(filename)
      // 一文字ずつ読み書きするので、非効率？
      Iterator.continually(is.read).takeWhile( -1 != _ ).foreach(os.write)
    }
  }

  /* パイプライン演算子(|>)は関数適用の記述の順番を入れ替える演算子
     f3(f2(f1(x))) → x |> f1 |> f2 |> f3 と書けるようになる
   */
  implicit class PipeLineOperator[T](x: T){ def |>[S] (f: T => S): S = f(x) }
}
