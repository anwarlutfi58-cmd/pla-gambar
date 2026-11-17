package com.pla.gambarcocok

data class EmojiFact(val emoji: String, val name: String, val fact: String)

object EmojiData {
    val all = listOf(
        EmojiFact("🐶", "Anjing", "Anjing memiliki hidung yang 10.000 kali lebih sensitif dari manusia."),
        EmojiFact("🐱", "Kucing", "Kucing menghabiskan 70% hidupnya untuk tidur."),
        EmojiFact("🐭", "Tikus", "Tikus bisa tertawa saat digelitik."),
        EmojiFact("🐹", "Hamster", "Hamster menyimpan makanan di pipi hingga 2x ukuran kepalanya."),
        EmojiFact("🐰", "Kelinci", "Gigi kelinci terus tumbuh sepanjang hidupnya."),
        EmojiFact("🦊", "Rubah", "Rubah menggunakan medan magnet Bumi untuk berburu."),
        EmojiFact("🐻", "Beruang", "Beruang bisa berlari 60 km/jam meski tubuhnya besar."),
        EmojiFact("🐼", "Panda", "Panda makan bambu hingga 14 jam sehari."),
        EmojiFact("🐨", "Koala", "Koala tidur 22 jam sehari."),
        EmojiFact("🐯", "Harimau", "Belang harimau unik seperti sidik jari manusia."),
        EmojiFact("🦁", "Singa", "Singa betina adalah pemburu utama di kelompoknya."),
        EmojiFact("🐮", "Sapi", "Sapi punya empat lambung untuk mencerna makanan."),
        EmojiFact("🍏", "Apel Hijau", "Apel hijau lebih asam karena kadar klorofil tinggi."),
        EmojiFact("🍎", "Apel Merah", "Apel mengapung di air karena 25% volumenya udara."),
        EmojiFact("🍐", "Pir", "Pir adalah buah tertua yang dibudidayakan manusia."),
        EmojiFact("🍊", "Jeruk", "Satu jeruk mengandung vitamin C untuk kebutuhan sehari."),
        EmojiFact("🍋", "Lemon", "Lemon mengandung lebih banyak gula daripada stroberi."),
        EmojiFact("🍌", "Pisang", "Pisang secara teknis adalah buah berry."),
        EmojiFact("🍉", "Semangka", "Semangka 92% terdiri dari air."),
        EmojiFact("🍇", "Anggur", "Anggur bisa difermentasi jadi wine sejak 6000 SM."),
        EmojiFact("🍓", "Stroberi", "Stroberi adalah satu-satunya buah dengan biji di luar."),
        EmojiFact("🫐", "Blueberry", "Blueberry bisa memperbaiki daya ingat."),
        EmojiFact("🍈", "Melon", "Melon berasal dari Afrika dan Asia Barat."),
        EmojiFact("🍒", "Ceri", "Ceri adalah buah yang berkerabat dengan mawar."),
        EmojiFact("🌵", "Kaktus", "Kaktus menyimpan air hingga ratusan liter."),
        EmojiFact("🌲", "Pinus", "Pinus bisa hidup hingga 5000 tahun."),
        EmojiFact("🌳", "Pohon", "Satu pohon besar menghasilkan oksigen untuk 2 orang."),
        EmojiFact("🌴", "Palem", "Pohon palem punya lebih dari 2600 spesies."),
        EmojiFact("🌱", "Tunas", "Tunas bisa tumbuh menembus aspal untuk mencari cahaya."),
        EmojiFact("🌿", "Herba", "Herba sudah dipakai sebagai obat sejak 5000 tahun lalu."),
        EmojiFact("☘️", "Semanggi", "Semanggi berdaun empat dianggap simbol keberuntungan."),
        EmojiFact("🍀", "Semanggi Hijau", "Semanggi berdaun tiga adalah lambang Irlandia."),
        EmojiFact("🌺", "Hibiskus", "Hibiskus mekar hanya satu hari."),
        EmojiFact("🌻", "Bunga Matahari", "Bunga matahari muda selalu menghadap Matahari."),
        EmojiFact("🌼", "Daisy", "Daisy adalah bunga yang bisa dimakan."),
        EmojiFact("🌷", "Tulip", "Belanda pernah mengalami 'tulip mania' pada 1637.")
    )

    fun factOf(emoji: String): String =
        all.firstOrNull { it.emoji == emoji }?.fact ?: "Gambar unik yang menarik!"
}