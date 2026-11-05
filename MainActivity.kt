package com.calculadora.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.MathContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvExpression: TextView

    private var current = "0"          // número que se está escribiendo / mostrando
    private var operand: BigDecimal? = null
    private var op: String? = null     // "+", "-", "*", "/"
    private var expression = ""
    private var startNew = false       // el próximo dígito reemplaza el valor actual
    private var error = false

    private val maxDigits = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvResult = findViewById(R.id.tvResult)
        tvExpression = findViewById(R.id.tvExpression)
        bindButtons(findViewById(R.id.root))
        refresh()
    }

    /** Asigna el listener a todos los botones usando su android:tag como tecla. */
    private fun bindButtons(v: View) {
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) bindButtons(v.getChildAt(i))
        } else if (v is Button) {
            v.setOnClickListener { onKey(v.tag as String) }
        }
    }

    private fun onKey(k: String) {
        when {
            k.length == 1 && k[0].isDigit() -> digit(k)
            k == "." -> decimal()
            k == "+" || k == "-" || k == "*" || k == "/" -> operator(k)
            k == "=" -> equals()
            k == "%" -> percent()
            k == "DEL" -> backspace()
            k == "C" -> clearAll()
        }
        refresh()
    }

    private fun digit(d: String) {
        if (error) clearAll()
        if (startNew) {
            current = "0"
            startNew = false
        }
        if (current.replace("-", "").replace(".", "").length >= maxDigits) return
        current = if (current == "0") d else current + d
    }

    private fun decimal() {
        if (error) clearAll()
        if (startNew) {
            current = "0"
            startNew = false
        }
        if (!current.contains(".")) current += "."
    }

    private fun operator(o: String) {
        if (error) return
        val cur = BigDecimal(current)
        if (op != null && !startNew) {
            // encadenar operaciones: 2 + 3 + ...
            val r = compute(operand!!, cur, op!!)
            if (r == null) {
                showDivZero()
                return
            }
            operand = r
            current = fmt(r)
        } else if (op == null) {
            operand = cur
        }
        op = o
        startNew = true
        expression = "${fmt(operand!!)} ${symbol(o)}"
    }

    private fun equals() {
        if (error) return
        val o = op ?: return
        val a = operand ?: return
        val b = BigDecimal(current)
        val r = compute(a, b, o)
        if (r == null) {
            showDivZero()
            return
        }
        expression = "${fmt(a)} ${symbol(o)} ${fmt(b)} ="
        current = fmt(r)
        operand = null
        op = null
        startNew = true
    }

    private fun percent() {
        if (error) return
        current = fmt(BigDecimal(current).divide(BigDecimal(100)))
        startNew = true
    }

    private fun backspace() {
        if (error) {
            clearAll()
            return
        }
        if (startNew) return
        current = current.dropLast(1)
        if (current.isEmpty() || current == "-") current = "0"
    }

    private fun clearAll() {
        current = "0"
        operand = null
        op = null
        expression = ""
        startNew = false
        error = false
    }

    private fun showDivZero() {
        current = getString(R.string.error_div_zero)
        expression = ""
        operand = null
        op = null
        error = true
        startNew = true
    }

    /** Devuelve null si hay división entre cero. */
    private fun compute(a: BigDecimal, b: BigDecimal, o: String): BigDecimal? = when (o) {
        "+" -> a.add(b)
        "-" -> a.subtract(b)
        "*" -> a.multiply(b)
        "/" -> if (b.signum() == 0) null else a.divide(b, MathContext.DECIMAL64)
        else -> b
    }

    private fun fmt(n: BigDecimal): String {
        var r = n.round(MathContext(12))
        if (r.signum() == 0) return "0"
        r = r.stripTrailingZeros()
        return r.toPlainString()
    }

    private fun symbol(o: String) = when (o) {
        "*" -> "×"
        "/" -> "÷"
        "-" -> "−"
        else -> o
    }

    private fun refresh() {
        tvResult.text = current
        tvExpression.text = expression
    }
}
