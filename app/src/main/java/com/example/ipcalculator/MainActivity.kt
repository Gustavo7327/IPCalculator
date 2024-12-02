package com.example.ipcalculator

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private var ipClass: String = ""
    private var networkAddress: String = ""
    private var firstHost: String = ""
    private var lastHost: String = ""
    private var broadcast: String = ""
    private var ipAvailability: String = ""
    private var numberOfSubNetworks: Int = 0
    private var hostsBySubNetwork: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val botaoCalcular: Button = findViewById(R.id.button)
        val progressCircle: ProgressBar = findViewById(R.id.progressBar)
        val tableLayout: TableLayout = findViewById(R.id.tableLayout)
        val editTextIP: EditText = findViewById(R.id.editTextIP)
        val editTextMascara: EditText = findViewById(R.id.editTextMask)



        // Configurar o evento de clique do botão
        botaoCalcular.setOnClickListener {
            val ip = editTextIP.text.toString()
            val mask = editTextMascara.text.toString()

            if (isValidIp(ip) && isValidIp(mask)) {
                // Mostrar a barra de progresso e ocultar tabela
                tableLayout.visibility = TableLayout.GONE
                progressCircle.visibility = ProgressBar.VISIBLE

                // Iniciar a tarefa assíncrona
                startTask(ip, mask, progressCircle, tableLayout)

            } else {
                Toast.makeText(this, "IP ou Máscara Inválidos", Toast.LENGTH_SHORT).show()
            }
        }

        // Adicionar comportamento do TextWatcher no EditText do IP
        editTextIP.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                val inputText = editable.toString()

                // Desabilitar temporariamente o TextWatcher para evitar loops recursivos
                editTextIP.removeTextChangedListener(this)

                // Impedir que o primeiro caractere seja um ponto
                if (inputText.startsWith(".")) {
                    editable?.replace(0, 1, "") // Remove o ponto do início
                }

                // Verificar e evitar dois pontos consecutivos
                if (inputText.contains("..")) {
                    // Encontrou dois pontos consecutivos, remover o segundo ponto
                    val index = inputText.indexOf("..")
                    editable?.replace(index, index + 2, ".")
                }

                // Restringir a entrada para apenas números e pontos
                if (inputText.contains(Regex("[^0-9.]"))) {
                    editable?.replace(0, editable.length, inputText.replace(Regex("[^0-9.]"), ""))
                }

                // Contar a quantidade de pontos no texto
                val dotCount = inputText.count { it == '.' }

                // Impedir mais de 3 pontos
                if (dotCount > 3) {
                    // Se houver mais de 3 pontos, remover o último ponto inserido
                    editable?.replace(inputText.length - 1, inputText.length, "")
                }

                // Verificar a quantidade de octetos e impedir mais de três
                val parts = inputText.split(".")
                if (parts.size > 4) {
                    // Se houver mais de 4 partes (contando com os octetos e os pontos), remova o último caractere inserido
                    editable?.replace(inputText.length - 1, inputText.length, "")
                }

                // Verificar se algum octeto é maior que 255
                val invalidPartIndex = parts.indexOfFirst { it.toIntOrNull()?.let { it > 255 } == true }
                if (invalidPartIndex != -1) {
                    // Se algum octeto for maior que 255, substituir o octeto inválido por 255
                    val correctedText = parts.mapIndexed { index, part ->
                        if (index == invalidPartIndex) {
                            "255"  // Substitui o octeto inválido por 255
                        } else {
                            part
                        }
                    }.joinToString(".")

                    // Substituir o texto no Editable
                    editable?.replace(0, inputText.length, correctedText)
                }

                // Adicionar ponto automaticamente após 3 números
                val lastPart = parts.lastOrNull()
                if (lastPart != null && lastPart.length == 3 && dotCount < 3) {
                    // Se o último octeto tiver 3 dígitos e o número de pontos for menor que 3
                    editable?.append(".")
                }

                // Reabilitar o TextWatcher
                editTextIP.addTextChangedListener(this)
            }
        })

        // Adicionar comportamento do TextWatcher no EditText da Máscara
        editTextMascara.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                val inputText = editable.toString()

                // Desabilitar temporariamente o TextWatcher para evitar loops recursivos
                editTextMascara.removeTextChangedListener(this)

                // Impedir que o primeiro caractere seja um ponto
                if (inputText.startsWith(".")) {
                    editable?.replace(0, 1, "") // Remove o ponto do início
                }

                // Verificar e evitar dois pontos consecutivos
                if (inputText.contains("..")) {
                    // Encontrou dois pontos consecutivos, remover o segundo ponto
                    val index = inputText.indexOf("..")
                    editable?.replace(index, index + 2, ".")
                }

                // Restringir a entrada para apenas números e pontos
                if (inputText.contains(Regex("[^0-9.]"))) {
                    editable?.replace(0, editable.length, inputText.replace(Regex("[^0-9.]"), ""))
                }

                // Contar a quantidade de pontos no texto
                val dotCount = inputText.count { it == '.' }

                // Impedir mais de 3 pontos
                if (dotCount > 3) {
                    // Se houver mais de 3 pontos, remover o último ponto inserido
                    editable?.replace(inputText.length - 1, inputText.length, "")
                }

                // Verificar a quantidade de octetos e impedir mais de três
                val parts = inputText.split(".")
                if (parts.size > 4) {
                    // Se houver mais de 4 partes (contando com os octetos e os pontos), remova o último caractere inserido
                    editable?.replace(inputText.length - 1, inputText.length, "")
                }

                // Verificar se algum octeto é maior que 255
                val invalidPartIndex = parts.indexOfFirst { it.toIntOrNull()?.let { it > 255 } == true }
                if (invalidPartIndex != -1) {
                    // Se algum octeto for maior que 255, substituir o octeto inválido por 255
                    val correctedText = parts.mapIndexed { index, part ->
                        if (index == invalidPartIndex) {
                            "255"  // Substitui o octeto inválido por 255
                        } else {
                            part
                        }
                    }.joinToString(".")

                    // Substituir o texto no Editable
                    editable?.replace(0, inputText.length, correctedText)
                }

                // Adicionar ponto automaticamente após 3 números
                val lastPart = parts.lastOrNull()
                if (lastPart != null && lastPart.length == 3 && dotCount < 3) {
                    // Se o último octeto tiver 3 dígitos e o número de pontos for menor que 3
                    editable?.append(".")
                }

                // Reabilitar o TextWatcher
                editTextMascara.addTextChangedListener(this)
            }
        })


    }

    // Função para rodar a tarefa assíncrona
    private fun startTask(ip: String, mask: String, progressCircle: ProgressBar, tableLayout: TableLayout) {
        CoroutineScope(Dispatchers.Default).launch {

            loadingData(ip,mask)

            withContext(Dispatchers.Main) {

                val ipAddress: TextView = findViewById(R.id.ip_address)
                val netmask: TextView = findViewById(R.id.mask)
                val networkAddressTextView: TextView = findViewById(R.id.network_address)
                val firstHostTextView: TextView = findViewById(R.id.first_host)
                val lastHostTextView: TextView = findViewById(R.id.last_host)
                val broadcastTextView: TextView = findViewById(R.id.broadcast)
                val ipClassTextView: TextView = findViewById(R.id.ip_class)
                val numberOfSubNetworksTextView: TextView = findViewById(R.id.sub_network_number)
                val hostsBySubNetworkTextView: TextView = findViewById(R.id.hosts_by_sub_network)
                val ipAvailableTextView: TextView = findViewById(R.id.ip_availability)

                // Preencher os campos com os resultados
                ipAddress.text = ip
                netmask.text = mask
                networkAddressTextView.text = networkAddress
                firstHostTextView.text = firstHost
                lastHostTextView.text = lastHost
                broadcastTextView.text = broadcast
                ipClassTextView.text = ipClass
                numberOfSubNetworksTextView.text = numberOfSubNetworks.toString()
                hostsBySubNetworkTextView.text = hostsBySubNetwork.toString()
                ipAvailableTextView.text = ipAvailability

                // Após a conclusão da tarefa, oculta a barra de progresso
                progressCircle.visibility = ProgressBar.GONE
                delay(1000) // Simula algum atraso, se necessário
                tableLayout.visibility = TableLayout.VISIBLE
            }
        }
    }

    private suspend fun loadingData(ip: String, mask: String){
        ipClass = Calculator.getIPClass(ip)
        networkAddress = Calculator.calculateNetwork(ip, mask)
        firstHost = Calculator.calculateFirstHost(networkAddress)
        broadcast = Calculator.calculateBroadcast(ip, mask).toString()
        lastHost = Calculator.calculateLastHost(broadcast)
        numberOfSubNetworks = Calculator.getNumberOfSubNetwork(mask)
        hostsBySubNetwork = Calculator.getNumberOfHostsBySubNetwork(mask)
        ipAvailability = Calculator.ipAvailability(ip)
        delay(3000)
    }

    // Função para validar o formato de um endereço IP
    fun isValidIp(ip: String): Boolean {
        // Expressão regular para validar IP
        val regex = "^([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})$".toRegex()
        val matchResult = regex.matchEntire(ip)
        if (matchResult != null) {
            // Verificar se cada número está entre 0 e 255
            return matchResult.groupValues.subList(1, 5).all { it.toInt() in 0..255 }
        }
        return false
    }




}
