package com.github.shadowsocks

import java.io.IOException

import android.app.Activity
import android.content.Intent
import android.os.{Bundle, Handler, Message}
import android.view.View
import android.widget.{Button, EditText, Toast}
import com.github.shadowsocks.utils.{Config, Key}
import org.json.{JSONException, JSONObject}

/**
 * Created by nothisboy on 15-10-4.
 */
class LoginActivity extends Activity with View.OnClickListener {

  lazy val EMAIL = "email"
  lazy val PASSWD = "passwd"
  lazy val REMEMBER_ME = "remember_me"

  lazy val MSG_LOGIN = 0x110
  lazy val MSG_USER_CONFIG = 0X111
  lazy val MSG_ERROR = 0x112

  var loginButton: Button = null
  var emailEdit: EditText = null
  var pwdEdit: EditText = null

  var responseHeaders: Bundle = null

  override def onCreate(saveINstanceState: Bundle) {
    super.onCreate(saveINstanceState)
    setContentView(R.layout.layout_login)

    loginButton = findViewById(R.id.loginButton).asInstanceOf[Button]
    emailEdit = findViewById(R.id.emailEdit).asInstanceOf[EditText]
    pwdEdit = findViewById(R.id.pwdEdit).asInstanceOf[EditText]

    loginButton.setOnClickListener(this);
  }

  override def onClick(view: View) {
    new Thread() {
      override def run() {
        val userBundle = new Bundle()
        userBundle.putString(EMAIL, emailEdit.getText().toString)
        userBundle.putString(PASSWD, pwdEdit.getText().toString)
        userBundle.putString(REMEMBER_ME, "week")

        val manager = new HttpManager()

        if (login(userBundle, manager)) {
          getUserConfig(manager)
        }
        else
          sendHandler("请检查您的网络连接", MSG_ERROR)


      }
    }.start()
  }

  private def login(userBundle: Bundle, manager: HttpManager): Boolean = {
    try {
      val result: String = manager.post(Api.Login, userBundle)
      if (result != null) {
        val jsonResult: JSONObject = new JSONObject(result)
        if (jsonResult.getString("code") == "1") {
          sendHandler(jsonResult.getString("msg"), MSG_LOGIN)
          return true
        }
      }
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
      case e: JSONException => {
        e.printStackTrace
      }
    }
    return false
  }

  private def getUserConfig(manager: HttpManager) {
    try {
      val config: String = manager.get(Api.getUserConfig)
      sendHandler(config, MSG_USER_CONFIG)
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
    }
  }

  private def sendHandler(str: String, msgWhat: Int) {
    val bundle: Bundle = new Bundle
    bundle.putString("msg", str)
    val msg: Message = new Message
    msg.what = msgWhat
    msg.setData(bundle)
    myHandler.sendMessage(msg)
  }

  var myHandler: Handler = new Handler() {
    override def handleMessage(msg: Message) {
      msg.what match {
        case MSG_LOGIN =>
          Toast.makeText(LoginActivity.this, msg.getData.getString("msg"), Toast.LENGTH_SHORT).show()
        case MSG_ERROR =>
          Toast.makeText(LoginActivity.this, msg.getData.getString("msg"), Toast.LENGTH_SHORT).show()
        case MSG_USER_CONFIG =>
          try {
            val jsonConfig: JSONObject = new JSONObject(msg.getData.getString("msg"))
            val intent: Intent = new Intent(LoginActivity.this, classOf[Shadowsocks])
            intent.putExtra(Key.proxy, Api.DOMAIN)
            intent.putExtra(Key.sitekey, jsonConfig.getString(Config.PASSWORD))
            intent.putExtra(Key.remotePort, jsonConfig.getString(Config.PORT))
            startActivity(intent)
          }
          catch {
            case e: JSONException => {
              e.printStackTrace
            }
          }
      }
    }
  }

}
