package com.zemachat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import org.telegram.messenger.UserConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class WaasEscrowActivity : Activity() {
    companion object {
        private const val BASE_URL = "https://waas-server.onrender.com"
        private const val PREFS = "zema_waas_prefs"
    }

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var statusTv: TextView
    private lateinit var contentBox: LinearLayout
    private lateinit var tabCreate: TextView
    private lateinit var tabOrder: TextView
    private lateinit var tabAdmin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(8))
        }
        bar.addView(TextView(this).apply {
            text = "<"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 22f
            setPadding(dp(10), dp(4), dp(14), dp(4))
            setOnClickListener { finish() }
        })
        bar.addView(TextView(this).apply {
            text = "Waas Escrow (Live)"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 18f
        })
        root.addView(bar)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1A1A1A"))
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        tabCreate = makeTab("Create") { showCreateTab() }
        tabOrder = makeTab("Order") { showOrderTab("") }
        tabAdmin = makeTab("Admin") { showAdminTab() }
        tabs.addView(tabCreate); tabs.addView(tabOrder); tabs.addView(tabAdmin)
        root.addView(tabs)

        statusTv = TextView(this).apply {
            setTextColor(Color.parseColor("#CCCCCC"))
            setBackgroundColor(Color.parseColor("#181818"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            textSize = 12f
            text = "Ready - Connected to Waas Server"
        }
        root.addView(statusTv)

        contentBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        root.addView(ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(contentBox)
        })
        setContentView(root)
        showCreateTab()
    }

    private fun makeTab(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 14f
        setPadding(dp(8), dp(8), dp(8), dp(8))
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { onClick() }
    }

    private fun highlightTab(active: TextView) {
        listOf(tabCreate, tabOrder, tabAdmin).forEach {
            it.setTextColor(if (it == active) Color.parseColor("#121212") else Color.parseColor("#D4AF37"))
            it.setBackgroundColor(if (it == active) Color.parseColor("#D4AF37") else Color.TRANSPARENT)
        }
    }

    private fun field(hintTxt: String, numeric: Boolean = false, initial: String = "") = EditText(this).apply {
        hint = hintTxt
        setText(initial)
        setHintTextColor(Color.parseColor("#888888"))
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1E1E1E"))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        if (numeric) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10)
        layoutParams = lp
    }

    private fun btn(label: String, gold: Boolean = true, onClick: () -> Unit) = Button(this).apply {
        text = label
        setBackgroundColor(Color.parseColor(if (gold) "#D4AF37" else "#2A2A2A"))
        setTextColor(Color.parseColor(if (gold) "#121212" else "#D4AF37"))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10)
        layoutParams = lp
        setOnClickListener { onClick() }
    }

    private fun getUserId(): String {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            val uid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()
            if (uid != 0L) "tg_$uid" else prefs.getString("uid", null) ?: ("u_" + UUID.randomUUID().toString().take(8)).also { prefs.edit().putString("uid", it).apply() }
        } catch (_: Exception) {
            prefs.getString("uid", null) ?: ("u_" + UUID.randomUUID().toString().take(8)).also { prefs.edit().putString("uid", it).apply() }
        }
    }

    private fun ensureToken(): String {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cached = prefs.getString("token", null)
        if (!cached.isNullOrBlank()) return cached
        val uid = getUserId()
        val res = rawRequest("POST", "/auth/dev-login", JSONObject().put("user_id", uid).put("name", uid), null, null)
        val tok = res.optString("token", "")
        if (tok.isBlank()) throw Exception(res.optString("error", "login_failed"))
        prefs.edit().putString("token", tok).apply()
        return tok
    }

    private fun rawRequest(method: String, path: String, body: JSONObject?, bearer: String?, adminTok: String?): JSONObject {
        val conn = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 45000
            readTimeout = 45000
            setRequestProperty("Content-Type", "application/json")
            if (!bearer.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearer")
            if (!adminTok.isNullOrBlank()) setRequestProperty("x-admin-token", adminTok)
            if (body != null) {
                doOutput = true
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            }
        }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() } ?: "{}"
        val json = try { JSONObject(text) } catch (_: Exception) { JSONObject().put("error", text) }
        if (conn.responseCode !in 200..299) {
            if (conn.responseCode == 401) getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("token").apply()
            throw Exception(json.optString("error", "HTTP ${conn.responseCode}"))
        }
        return json
    }

    private fun runApi(msg: String, block: () -> Unit) {
        statusTv.text = msg
        io.execute {
            try {
                block()
            } catch (e: Exception) {
                main.post {
                    statusTv.text = "Error: ${e.message}"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showCreateTab() {
        highlightTab(tabCreate)
        contentBox.removeAllViews()
        val itemEt = field("Item Name")
        val priceEt = field("Price (ETB)", true)
        val delivEt = field("Delivery Fee (ETB)", true, "0")
        val routeEt = field("Route (e.g. Merkato -> Hawassa)")
        val resultTv = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f; setPadding(0, dp(8), 0, dp(8)) }
        var lastCard = ""

        val sendBtn = btn("Send Order Card to Chat", false) {
            if (lastCard.isBlank()) return@btn
            val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, lastCard) }
            startActivity(Intent.createChooser(i, "Send Waas Order"))
        }.apply { visibility = View.GONE }

        val createBtn = btn("Create Live Escrow Order") {
            val item = itemEt.text.toString().trim()
            val price = priceEt.text.toString().trim()
            val deliv = delivEt.text.toString().trim().ifEmpty { "0" }
            val route = routeEt.text.toString().trim()
            if (item.isEmpty() || price.isEmpty()) { Toast.makeText(this, "Enter item and price", Toast.LENGTH_SHORT).show(); return@btn }
            runApi("Creating order on Waas server (may take 20s if waking)...") {
                val tok = ensureToken()
                val res = rawRequest("POST", "/orders", JSONObject().put("item", item).put("price", price).put("delivery", deliv).put("route", route), tok, null)
                val oid = res.getString("id")
                val am = res.getJSONObject("amounts")
                val pi = res.optJSONObject("payment_instructions")
                val card = buildString {
                    append("WAAS ESCROW ORDER\n")
                    append("Order ID: ").append(oid).append("\n")
                    append("Item: ").append(item).append("\n")
                    if (route.isNotEmpty()) append("Route: ").append(route).append("\n")
                    append("Buyer Total: ").append(am.getJSONObject("buyer_total").getString("etb")).append(" ETB\n")
                    if (pi != null) {
                        append("Bank: ").append(pi.optString("bank_name")).append("\n")
                        append("Account: ").append(pi.optString("account_number")).append(" (").append(pi.optString("account_name")).append(")\n")
                    }
                    append("Pay & Track Link: https://waas-server.onrender.com/order/").append(oid).append("\nOpen link in browser (WhatsApp / Telegram / Zema Chat)")
                }
                main.post {
                    statusTv.text = "Order Created: $oid"
                    lastCard = card
                    resultTv.text = card
                    sendBtn.visibility = View.VISIBLE
                }
            }
        }
        contentBox.addView(itemEt); contentBox.addView(priceEt); contentBox.addView(delivEt); contentBox.addView(routeEt)
        contentBox.addView(createBtn); contentBox.addView(resultTv); contentBox.addView(sendBtn)
    }

    private fun showOrderTab(prefillId: String) {
        highlightTab(tabOrder)
        contentBox.removeAllViews()
        val idEt = field("Paste Order ID", false, prefillId)
        val detailsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun loadOrder(oid: String) {
            if (oid.isBlank()) return
            runApi("Loading order $oid...") {
                val tok = ensureToken()
                val o = rawRequest("GET", "/orders/$oid", null, tok, null)
                main.post {
                    statusTv.text = "Order $oid: ${o.optString("status")}"
                    detailsBox.removeAllViews()
                    val am = o.getJSONObject("amounts")
                    val st = o.optString("status")
                    val role = o.optString("role")
                    val info = StringBuilder()
                    info.append("Status: ").append(st).append(" (").append(role).append(")\n")
                    info.append("Item: ").append(o.optString("item")).append("\n")
                    info.append("Buyer Total: ").append(am.getJSONObject("buyer_total").getString("etb")).append(" ETB\n")
                    info.append("Seller Payout: ").append(am.getJSONObject("seller_payout").getString("etb")).append(" ETB\n")
                    val pi = o.optJSONObject("payment_instructions")
                    if (pi != null) {
                        info.append("\nPay directly via CBE:\n")
                        info.append(pi.optString("bank_name")).append("\n")
                        info.append("Acc: ").append(pi.optString("account_number")).append("\n")
                        info.append("Name: ").append(pi.optString("account_name")).append("\n")
                        info.append("Amount: ").append(pi.optString("amount_etb")).append(" ETB\n")
                    }
                    detailsBox.addView(TextView(this).apply { text = info.toString(); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1A1A1A")); setPadding(dp(12), dp(12), dp(12), dp(12)) })

                    if (st == "OPEN" && role != "seller") {
                        val ftEt = field("Enter CBE FT Reference (e.g. FT26...)")
                        detailsBox.addView(ftEt)
                        detailsBox.addView(btn("Submit CBE FT Reference") {
                            val ft = ftEt.text.toString().trim()
                            runApi("Submitting CBE FT ref...") {
                                val r = rawRequest("POST", "/orders/$oid/pay", JSONObject().put("ft_ref", ft), ensureToken(), null)
                                main.post { statusTv.text = "Submitted FT: ${r.optString("ft_ref")} (Waiting Admin Confirm)"; Toast.makeText(this, "Submitted! Waiting for admin confirmation.", Toast.LENGTH_LONG).show() }
                            }
                        })
                    }
                    if (st == "FUNDED" && role == "seller") {
                        detailsBox.addView(btn("Mark Shipped") {
                            runApi("Marking shipped...") { rawRequest("POST", "/orders/$oid/ship", JSONObject(), ensureToken(), null); main.post { loadOrder(oid) } }
                        })
                    }
                    if (st == "SHIPPED" && role == "buyer") {
                        detailsBox.addView(btn("Show 6-Digit Delivery OTP") {
                            runApi("Fetching OTP...") {
                                val r = rawRequest("GET", "/orders/$oid/otp", null, ensureToken(), null)
                                main.post { statusTv.text = "Delivery OTP: ${r.optString("otp")}"; Toast.makeText(this, "OTP: ${r.optString("otp")}", Toast.LENGTH_LONG).show() }
                            }
                        })
                    }
                    if (st == "SHIPPED" && role == "seller") {
                        val otpEt = field("Enter Buyer's 6-Digit OTP", true)
                        detailsBox.addView(otpEt)
                        detailsBox.addView(btn("Confirm Delivery with OTP") {
                            runApi("Verifying OTP...") { rawRequest("POST", "/orders/$oid/confirm-delivery", JSONObject().put("otp", otpEt.text.toString().trim()), ensureToken(), null); main.post { loadOrder(oid) } }
                        })
                    }
                    if (st == "INSPECTION" && role == "buyer") {
                        detailsBox.addView(btn("Accept & Release Funds") {
                            runApi("Releasing funds...") { rawRequest("POST", "/orders/$oid/accept", JSONObject(), ensureToken(), null); main.post { loadOrder(oid) } }
                        })
                    }
                }
            }
        }

        contentBox.addView(idEt)
        contentBox.addView(btn("Load Order") { loadOrder(idEt.text.toString().trim()) })
        contentBox.addView(detailsBox)
        if (prefillId.isNotBlank()) loadOrder(prefillId)
    }

    private fun showAdminTab() {
        highlightTab(tabAdmin)
        contentBox.removeAllViews()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tokEt = field("Admin Token (saved locally only)", false, prefs.getString("admin_token", "") ?: "")
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun loadQueue() {
            val atok = tokEt.text.toString().trim()
            if (atok.isBlank()) { Toast.makeText(this, "Enter Admin Token first", Toast.LENGTH_SHORT).show(); return }
            prefs.edit().putString("admin_token", atok).apply()
            runApi("Loading pending CBE payments...") {
                val q = rawRequest("GET", "/admin/queue", null, null, atok)
                val arr = q.optJSONArray("pending_cbe_payments")
                main.post {
                    statusTv.text = "Pending CBE payments: ${arr?.length() ?: 0}"
                    listBox.removeAllViews()
                    if (arr == null || arr.length() == 0) {
                        listBox.addView(TextView(this).apply { text = "No pending CBE payments."; setTextColor(Color.parseColor("#AAAAAA")); setPadding(0, dp(12), 0, dp(12)) })
                        return@post
                    }
                    for (i in 0 until arr.length()) {
                        val p = arr.getJSONObject(i)
                        val ref = p.getString("tx_ref")
                        val oid = p.getString("order_id")
                        val etb = String.format(Locale.US, "%.2f", p.getLong("amount") / 100.0)
                        val card = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setBackgroundColor(Color.parseColor("#1E1E1E"))
                            setPadding(dp(12), dp(10), dp(12), dp(10))
                            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                            lp.bottomMargin = dp(10)
                            layoutParams = lp
                        }
                        card.addView(TextView(this).apply { text = "FT: $ref\nOrder: $oid | Amount: $etb ETB"; setTextColor(Color.WHITE); setPadding(0, 0, 0, dp(8)) })
                        card.addView(btn("Confirm CBE Received") {
                            runApi("Confirming $ref...") { rawRequest("POST", "/admin/payments/$ref/confirm", JSONObject(), null, atok); main.post { loadQueue() } }
                        })
                        card.addView(btn("Reject", false) {
                            runApi("Rejecting $ref...") { rawRequest("POST", "/admin/payments/$ref/reject", JSONObject(), null, atok); main.post { loadQueue() } }
                        })
                        listBox.addView(card)
                    }
                }
            }
        }

        contentBox.addView(tokEt)
        contentBox.addView(btn("Load Pending CBE Payments") { loadQueue() })
        contentBox.addView(listBox)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
