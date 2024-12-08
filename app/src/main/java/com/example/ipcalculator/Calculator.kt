package com.example.ipcalculator

import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.math.pow

class Calculator {

    companion object {

        private fun ipToBinary(ip: String): String {
            return ip.split(".").joinToString(".") { Integer.toBinaryString(it.toInt()).padStart(8, '0') }
        }

        private fun binaryToIp(binary: String): String {
            return binary.split(".").joinToString(".") { Integer.parseInt(it, 2).toString() }
        }

        private fun calculateBorrowedBits(original: String, newMask: String): Int {
            val originalBits = original.replace(".", "")
            val newBits = newMask.replace(".", "")

            var borrowedBits = 0
            for (i in originalBits.indices) {
                if (originalBits[i] == '0' && newBits[i] == '1') {
                    borrowedBits++
                }
            }
            return borrowedBits
        }

        private fun getNumberOfNetworkBits(mask: String): Int {
            return mask.split(".")
                .map { it.toInt() }
                .sumOf { Integer.bitCount(it) }
        }


        fun getIPClass(ip: String): String {
            val firstOctet = ip.substring(0, ip.indexOf(".")).toInt()
            val ipClass = when (firstOctet) {
                in 0..127 -> "Classe A"
                in 128..191 -> "Classe B"
                in 192..223 -> "Classe C"
                in 224..239 -> "Classe D"
                in 240..255 -> "Classe E"
                else -> "Classe desconhecida"
            }
            return ipClass
        }


        fun calculateNetwork(ip: String, mask: String): String {
            val ipBinary = ipToBinary(ip)
            val maskBinary = ipToBinary(mask)

            // Calcular Endereço de Rede (AND entre IP e Máscara)
            val networkBinary = ipBinary.split(".")
                .zip(maskBinary.split("."))
                .map { Integer.toBinaryString(Integer.parseInt(it.first, 2) and Integer.parseInt(it.second, 2)).padStart(8, '0') }
                .joinToString(".")
            return binaryToIp(networkBinary)
        }


        fun calculateFirstHost(network: String): String {
            return network.split(".").let {
                it[0] + "." + it[1] + "." + it[2] + "." + (it[3].toInt() + 1)
            }
        }


        fun calculateLastHost(broadcast: String): String {
            return broadcast.split(".").let {
                it[0] + "." + it[1] + "." + it[2] + "." + (it[3].toInt() - 1)
            }
        }


        fun calculateBroadcast(ip: String, subnetMask: String): String? {
            try {
                // Convertendo o IP e a máscara para InetAddress
                val ipAddress = InetAddress.getByName(ip)
                val subnet = InetAddress.getByName(subnetMask)

                // Convertendo os IPs para bytes
                val ipBytes = ipAddress.address
                val subnetBytes = subnet.address

                // Calculando o endereço de rede
                val networkBytes = ipBytes.zip(subnetBytes) { ipByte, subnetByte ->
                    ipByte.toInt() and subnetByte.toInt()
                }.map { it.toByte() }.toByteArray()

                // Invertendo a máscara de sub-rede
                val invertedSubnetBytes = subnetBytes.map { it.toInt().inv().toByte() }.toByteArray()

                // Calculando o endereço de broadcast
                val broadcastBytes = networkBytes.zip(invertedSubnetBytes) { networkByte, invertedSubnetByte ->
                    (networkByte.toInt() or invertedSubnetByte.toInt()).toByte()
                }.toByteArray()

                // Convertendo o resultado para string
                return InetAddress.getByAddress(broadcastBytes).hostAddress
            } catch (e: UnknownHostException) {
                throw IllegalArgumentException("Invalid IP or subnet mask")
            }
        }



        fun getNumberOfSubNetwork(mask: String, ip: String): Int {
            // Determinar a máscara original da classe do IP
            val ipClass = getIPClass(ip)
            val originalMask = when (ipClass) {
                "Classe A" -> "255.0.0.0"
                "Classe B" -> "255.255.0.0"
                "Classe C" -> "255.255.255.0"
                else -> throw IllegalArgumentException("IP inválido para cálculo de sub-redes.")
            }

            // Obter o número de bits na máscara original e na nova máscara
            val originalMaskBits = getNumberOfNetworkBits(originalMask)
            val newMaskBits = getNumberOfNetworkBits(mask)

            // Calcular os bits emprestados
            val borrowedBits = newMaskBits - originalMaskBits
            if (borrowedBits <= 0) return 1

            // Calcular o número de sub-redes
            return 2.0.pow(borrowedBits).toInt()
        }



        fun getNumberOfHostsBySubNetwork(mask: String): Int {
            val totalBitsForHosts = 32 - getNumberOfNetworkBits(mask)
            return 2.0.pow(totalBitsForHosts).toInt() - 2
        }


        fun ipAvailability(ip: String): String {
            val ipParts = ip.split(".")

            val firstOctet = ipParts[0].toInt()
            val secondOctet = ipParts[1].toInt()

            return when {
                firstOctet == 10 -> "Privado"
                firstOctet == 172 && secondOctet in 16..31 -> "Privado"
                firstOctet == 192 && secondOctet == 168 -> "Privado"
                else -> "Público"
            }

        }
    }
}