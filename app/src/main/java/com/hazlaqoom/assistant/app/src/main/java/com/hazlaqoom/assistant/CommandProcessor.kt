package com.hazlaqoom.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import java.util.*

class CommandProcessor(private val context: Context) {

    private val reminderManager = ReminderManager(context)

    fun execute(command: String): String {
        val lowerCmd = command.lowercase(Locale.getDefault())

        return when {
            lowerCmd.contains("تذكير ماء") || lowerCmd.contains("شرب ماء") || lowerCmd.contains("water reminder") -> {
                reminderManager.scheduleWaterReminders(2)
                "تم تفعيل تذكير شرب الماء كل ساعتين"
            }

            lowerCmd.contains("فحص صحي") || lowerCmd.contains("health check") || lowerCmd.contains("كيف حالك") -> {
                reminderManager.scheduleHealthCheck(20, 0)
                "تم تفعيل الفحص الصحي المسائي"
            }

            lowerCmd.contains("الغاء التذكير") || lowerCmd.contains("cancel reminder") || lowerCmd.contains("ايقاف التذكير") -> {
                reminderManager.cancelWaterReminders()
                reminderManager.cancelHealthCheck()
                "تم إلغاء جميع التذكيرات"
            }

            lowerCmd.contains("اتصل") || lowerCmd.contains("اتصال") || lowerCmd.contains("كلم") -> {
                makePhoneCall(lowerCmd)
            }

            lowerCmd.contains("ارسل رسالة") || lowerCmd.contains("ابعت رسالة") || lowerCmd.contains("اكتب رسالة") -> {
                sendSMS(lowerCmd)
            }

            lowerCmd.contains("افتح") || lowerCmd.contains("شغل") -> {
                openApp(lowerCmd)
            }

            lowerCmd.contains("صورة") || lowerCmd.contains("كاميرا") || lowerCmd.contains("افتح الكاميرا") -> {
                openCamera()
            }

            lowerCmd.contains("واي فاي") || lowerCmd.contains("wifi") -> {
                toggleWifi(lowerCmd)
            }

            lowerCmd.contains("بلوتوث") || lowerCmd.contains("bluetooth") -> {
                toggleBluetooth(lowerCmd)
            }

            lowerCmd.contains("سطوع") || lowerCmd.contains("brightness") -> {
                adjustBrightness(lowerCmd)
            }

            lowerCmd.contains("صوت") || lowerCmd.contains("volume") || lowerCmd.contains("رفع الصوت") || lowerCmd.contains("خفض الصوت") -> {
                adjustVolume(lowerCmd)
            }

            lowerCmd.contains("منبه") || lowerCmd.contains("المنبه") || lowerCmd.contains("صحيني") -> {
                setAlarm(lowerCmd)
            }

            lowerCmd.contains("ابحث") || lowerCmd.contains("بحث") || lowerCmd.contains("دور على") -> {
                searchWeb(lowerCmd)
            }

            lowerCmd.contains("موقعي") || lowerCmd.contains("أين أنا") || lowerCmd.contains("خريطة") -> {
                openMaps()
            }

            lowerCmd.contains("الساعة") || lowerCmd.contains("الوقت") || lowerCmd.contains("كم الساعة") -> {
                tellTime()
            }

            lowerCmd.contains("التاريخ") || lowerCmd.contains("اليوم") -> {
                tellDate()
            }

            lowerCmd.contains("الإعدادات") || lowerCmd.contains("settings") -> {
                openSettings()
            }

            lowerCmd.contains("مرحبا") || lowerCmd.contains("هلا") || lowerCmd.contains("السلام عليكم") || lowerCmd.contains("صباح") || lowerCmd.contains("مساء") -> {
                greetUser()
            }

            lowerCmd.contains("مساعدة") || lowerCmd.contains("help") || lowerCmd.contains("ماذا تستطيع") -> {
                getHelp()
            }

            else -> {
                "عذراً، لم أفهم الأمر. جرب: تذكير ماء، فحص صحي، افتح الكاميرا، أو اتصل بأحمد"
            }
        }
    }

    private fun makePhoneCall(command: String): String {
        val number = extractNumber(command)
        return if (number.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                "جاري الاتصال بـ $number"
            } catch (e: SecurityException) {
                "يرجى منح إذن الاتصال"
            }
        } else {
            "لم أفهم الرقم، قل: اتصل بـ ٠٥٥٥٥٥٥٥٥٥"
        }
    }

    private fun sendSMS(command: String): String {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري فتح تطبيق الرسائل"
    }

    private fun openApp(command: String): String {
        val appName = command.replace("افتح", "").replace("شغل", "").trim()
        val packageName = when {
            appName.contains("يوتيوب") || appName.contains("youtube") -> "com.google.android.youtube"
            appName.contains("واتس") || appName.contains("whatsapp") -> "com.whatsapp"
            appName.contains("تلجرام") || appName.contains("telegram") -> "org.telegram.messenger"
            appName.contains("انستغرام") || appName.contains("instagram") -> "com.instagram.android"
            appName.contains("فيس") || appName.contains("facebook") -> "com.facebook.katana"
            appName.contains("تويتر") || appName.contains("x") || appName.contains("twitter") -> "com.twitter.android"
            appName.contains("خرائط") || appName.contains("maps") -> "com.google.android.apps.maps"
            appName.contains("متصفح") || appName.contains("chrome") -> "com.android.chrome"
            appName.contains("موسيقى") || appName.contains("spotify") -> "com.spotify.music"
            appName.contains("بريد") || appName.contains("gmail") -> "com.google.android.gm"
            appName.contains("تقويم") || appName.contains("calendar") -> "com.google.android.calendar"
            appName.contains("حاسبة") || appName.contains("calculator") -> "com.google.android.calculator"
            appName.contains("ساعة") || appName.contains("clock") -> "com.google.android.deskclock"
            appName.contains("إعدادات") || appName.contains("settings") -> "com.android.settings"
            else -> null
        }

        return if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "جاري فتح $appName"
            } else {
                "التطبيق غير مثبت"
            }
        } else {
            "لم أعرف التطبيق، جرب: افتح يوتيوب، أو واتساب"
        }
    }

    private fun openCamera(): String {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري فتح الكاميرا"
    }

    private fun toggleWifi(command: String): String {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return if (command.contains("شغل") || command.contains("فعل")) {
            "افتح إعدادات الواي فاي لتفعيله"
        } else if (command.contains("اطفئ") || command.contains("أوقف")) {
            "افتح إعدادات الواي فاي لإيقافه"
        } else {
            "جاري فتح إعدادات الواي فاي"
        }
    }

    private fun toggleBluetooth(command: String): String {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري فتح إعدادات البلوتوث"
    }

    private fun adjustBrightness(command: String): String {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري فتح إعدادات السطوع"
    }

    private fun adjustVolume(command: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when {
            command.contains("رفع") || command.contains("اعلى") || command.contains("up") -> {
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "تم رفع الصوت"
            }
            command.contains("خفض") || command.contains("اقل") || command.contains("down") -> {
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "تم خفض الصوت"
            }
            command.contains("صامت") || command.contains("silent") || command.contains("mute") -> {
                audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                "تم كتم الصوت"
            }
            else -> {
                audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                "جاري ضبط الصوت"
            }
        }
    }

    private fun setAlarm(command: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "منبه حذلقوم")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري فتح تطبيق المنبه"
    }

    private fun searchWeb(command: String): String {
        val query = command.replace("ابحث", "").replace("بحث", "").replace("دور على", "").trim()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري البحث عن: $query"
    }

    private fun openMaps(): String {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=موقعي"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري فتح الخريطة"
    }

    private fun tellTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return "الساعة الآن ${hour}:${String.format("%02d", minute)}"
    }

    private fun tellDate(): String {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "الأحد"
            Calendar.MONDAY -> "الإثنين"
            Calendar.TUESDAY -> "الثلاثاء"
            Calendar.WEDNESDAY -> "الأربعاء"
            Calendar.THURSDAY -> "الخميس"
            Calendar.FRIDAY -> "الجمعة"
            Calendar.SATURDAY -> "السبت"
            else -> ""
        }
        return "اليوم $dayOfWeek ${day}/${month}/${year}"
    }

    private fun openSettings(): String {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "جاري فتح الإعدادات"
    }

    private fun greetUser(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..11 -> "صباح الخير! كيف يمكنني مساعدتك اليوم؟"
            hour in 12..16 -> "مساء النور! ماذا تحتاج؟"
            hour in 17..21 -> "مساء الخير! أنا هنا لمساعدتك"
            else -> "مرحباً! كيف يمكنني مساعدتك؟"
        }
    }

    private fun getHelp(): String {
        return "أستطيع مساعدتك في: تذكير ماء، فحص صحي، الاتصال، الرسائل، فتح تطبيقات، الكاميرا، البحث، الواي فاي، البلوتوث، السطوع، الصوت، المنبه، الموقع، الوقت، والتاريخ"
    }

    private fun extractNumber(command: String): String {
        val regex = Regex("[0-9٠١٢٣٤٥٦٧٨٩]+")
        val match = regex.find(command)
        return match?.value?.replace('٠', '0')
            ?.replace('١', '1')
            ?.replace('٢', '2')
            ?.replace('٣', '3')
            ?.replace('٤', '4')
            ?.replace('٥', '5')
            ?.replace('٦', '6')
            ?.replace('٧', '7')
            ?.replace('٨', '8')
            ?.replace('٩', '9') ?: ""
    }
}
