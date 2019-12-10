package com.x2h.rules

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.jaredrummler.android.shell.Shell

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.SocketException

class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {

    lateinit var preferences: SharedPreferences
    lateinit var view: View
    val util = Util(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        view = main_content.rootView
        initData()
        initView()
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        val name = when (buttonView?.id) {
            switch_meituan.id -> getString(R.string.name_meituan)
            switch_crtip.id -> getString(R.string.name_xiecheng)
            switch_dianping.id -> getString(R.string.name_dianping)
            else -> getString(R.string.name_meituan)
        }
        val edit = preferences.edit()
        setRule(name, isChecked)
        edit.putBoolean("status-${name}", isChecked)
        edit.apply()
    }

    /**
     * 解析规则
     * @return 包含 name、cmd 的 Map 对象
     */
    private fun parser(): Map<String, String> {
        val retval = HashMap<String, String>()
        val rules = preferences.getString("rules", "") as String
        if (rules.isEmpty()) {
//            Snackbar.make(main_content, "程序运行异常，请重启程序以便于初始化数据!", Snackbar.LENGTH_LONG).show()
            initData()
//            parser()
        } else {
            val obj = JSONObject(rules)
            val array = obj.getJSONArray("data")
            for (i in 0 until array.length()) {
                val _obj = array.getJSONObject(i)
                retval[_obj.getString("name")] = _obj.getString("cmd")
            }
        }
        return retval
    }

    /**
     * 执行命令
     * @param name 平台名
     * @param status 是否已经开启
     */
    private fun setRule(name: String, status: Boolean) {
        val rules = preferences.getString("rules", "")
        if (rules?.isNotEmpty() == false)
            puts(
                getString(R.string.tag_error), getString(R.string.get_rule_failed),
                "#FF0000", "#FFE500"
            )
        else {
            var cmd = parser()[name]
            if (cmd != null) {
                if (cmd.isNotEmpty()) {
                    if (!status) cmd = cmd.replace("-A", "-D")
                    execute(cmd)
                }
            } else run {
                puts(
                    getString(R.string.tag_error),
                    getString(R.string.not_found_rule),
                    "#FF0000",
                    "#111111"
                )
            }
            // 将scroll view显示到最底部
            sv_output.post {
                sv_output.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    /**
     * 执行命令
     * @param cmd 命令
     * @return 命令输出信息
     */
    private fun execute(cmd: String, isPrint: Boolean = true): String {
        var msg = ""
        if (isRooted()) {
            val t = Thread {
                if (isPrint)
                    putsOnUi(getString(R.string.tag_command), cmd, "#FF0000", "#111111")
                val cr = Shell.SU.run(cmd)
                msg = getString(R.string.execute_failed).format(cr.getStderr())
                if (cr.isSuccessful) {
                    msg = cr.getStdout()
                }
                if (isPrint)
                    putsOnUi(
                        getString(R.string.tag_out),
                        if (msg.isNotEmpty()) msg else "Done.",
                        "#FF0000",
                        "#111111"
                    )
                uiThread {
                    // 将scroll view显示到最底部
                    sv_output.post {
                        sv_output.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
            t.start()
            t.join()
        }
        return msg
    }

    /**
     * 初始化view
     */
    private fun initView() {
        fab.setOnClickListener { _ ->
            //            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("1", null).show()
            cleanRules()
        }

        if (preferences.getBoolean(
                getString(R.string.status_xiecheng),
                false
            )
        ) switch_crtip.isChecked = true
        if (preferences.getBoolean(
                getString(R.string.status_meituan),
                false
            )
        ) switch_meituan.isChecked = true
        if (preferences.getBoolean(
                getString(R.string.status_dianping),
                false
            )
        ) switch_dianping.isChecked = true

        // 添加选择更改监听事件
        switch_crtip.setOnCheckedChangeListener(this)
        switch_dianping.setOnCheckedChangeListener(this)
        switch_meituan.setOnCheckedChangeListener(this)
    }


    /**
     * 初始化数据
     */
    private fun initData() {
        // 首先拿到 SharedPreferences 对象
        preferences = applicationContext.getSharedPreferences("rule", 0) ?: return


//        if (!Shell.SU.run("su").isSuccessful) {
//            Snackbar.make(view, getString(R.string.cant_root_msg), Snackbar.LENGTH_LONG)
//                .setAction(getString(R.string.retry_root), null).show()
//            isRoot = false
//        }
        // 判断有没有网络连接
        if (!util.isNetworkConnected()) {
            AlertDialog.Builder(this).setTitle(getString(R.string.no_network_connected))
                .setMessage(getString(R.string.no_network_connected_msg))
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        } else {
            if (isRooted()) {
                // with 是个内联函数，接收一个对象做为参数，在函数块中使用this指代该对象，可以调用其成员函数
                with(preferences.edit()) {
                    // doAsync是创建一个线程
                    doAsync {
                        // puts的多态函数，在runOnUiThread中调用puts函数，因为Android不允许在线程中操作view
                        putsOnUi(
                            getString(R.string.init), getString(R.string.request_ing),
                            "#FF0000", "#FF00FF00"
                        )
                        // 从服务器获取数据
                        val body = request()
                        if (body == "error") {
                            Snackbar.make(main_content, "获取规则失败，请重试或尝试联系作者！", Snackbar.LENGTH_LONG).show()
                            return@doAsync
                        }
                        // runOnUiThread
                        uiThread {
                            // preferences的成员函数putString
                            putString("rules", body)
                            // 调用 apply 提交更改
                            apply()
                            // 如果程序是第一次安装启动，就清空所有规则
                            if (preferences.getBoolean("f_i_r", true)) {
                                putBoolean("f_i_r", false)
                                cleanRules()
                            }
                            puts(
                                getString(R.string.init), getString(R.string.init_done),
                                getString(R.string.init_color), "#FF00FF00"
                            )
                            puts("", "-------------------------", "#111111", "#111111")
                        }
                    }
                }
            }
        }
    }

    private fun isRooted(): Boolean {
        var root = false
        if (!Shell.SU.run("su").isSuccessful) {
            Snackbar.make(main_content, "你好像还没赋予Rules Root权限！", Snackbar.LENGTH_LONG)
                .setAction("点我获取") {
                    val cr = Shell.SU.run("su")
                    if (cr.isSuccessful) {
                        root = true
                    } else {
                        Toast.makeText(applicationContext, "获取root权限失败!", Toast.LENGTH_LONG).show()
                    }
                }.show()
        } else {
            root = true
        }
        return root
    }

    /**
     * 清空所有规则
     */
    private fun cleanRules() {
        if (isRooted()) {
            with(parser()) {
                if (size > 0) {
                    keys.forEach(fun(key: String) {
                        while (true) {
                            putsOnUi(
                                getString(R.string.init),
                                getString(R.string.clean_rule).format(key),
                                getString(R.string.init_color),
                                "#FF00FF00"
                            )
                            val cmd = get(key) as String
                            val msg = execute(cmd.replace("-A", "-D"), false)
                            if ("Bad rule" in msg) {
                                // 清空之后输出一下信息
                                putsOnUi(
                                    getString(R.string.init),
                                    getString(R.string.clean_done).format(key),
                                    getString(R.string.init_color),
                                    "#FF00FF00"
                                )
                                // 然后把对应的switch给关了
                                uiThread {
                                    when (key) {
                                        getString(R.string.name_dianping) -> switch_dianping.isChecked =
                                            false
                                        getString(R.string.name_meituan) -> switch_meituan.isChecked =
                                            false
                                        getString(R.string.name_xiecheng) -> switch_crtip.isChecked =
                                            false
                                    }
                                    // 将scroll view显示到最底部
                                    sv_output.post {
                                        sv_output.fullScroll(View.FOCUS_DOWN)
                                    }
                                }
                                // 并把sp中的状态改为false
                                val ed = preferences.edit()
                                ed.putBoolean("status-${key}", false)
                                ed.apply()
                                break
                            }
                        }
                    })
                }
            }
        }
    }

    /**
     * 添加输出到 TextView
     */
    private fun putsOnUi(prefix: String, content: String, p_color: String, c_color: String) {
        runOnUiThread {
            puts(prefix, content, p_color, c_color)
        }
    }

    /**
     * 添加输出到 TextView
     * @param prefix 前缀
     * @param content 内容
     * @param p_color 前缀颜色
     * @param c_color 内容颜色
     */
    private fun puts(prefix: String, content: String, p_color: String, c_color: String) {
        val out = getString(R.string.log_format).format(p_color, prefix, c_color, content)
        val sp: Spanned
        sp = if (Build.VERSION.SDK_INT >= 24)
            Html.fromHtml(out, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(out)
        tv_output.append(sp)
    }

    private fun doAsync(f: () -> Unit) = Thread { f() }.start()

    private fun uiThread(f: () -> Unit) = runOnUiThread { f() }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * 网络请求
     */
    private fun request(): String {
        val request = Request.Builder().url(getString(R.string.api_url)).build()
        val client = OkHttpClient()
        var error: String
        try {
            val resp = client.newCall(request).execute()
            error = getString(R.string.request_error).format(resp.code)
            if (resp.isSuccessful) return resp.body?.string() ?: return error
        } catch (e: SocketException) {
            error = "error"
        }
        return error
    }

}
