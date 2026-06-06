package com.example.ui.theme

object Translation {
    private val en = mapOf(
        "title_browser" to "Browser",
        "title_downloads" to "Downloads",
        "title_files" to "Saved Files",
        "title_settings" to "Settings",
        "placeholder_url" to "Search or enter direct URL",
        "btn_download" to "Download",
        "tab_all" to "All",
        "tab_video" to "Videos",
        "tab_audio" to "Audios",
        "quality_selection" to "Select Quality",
        "confirm_download" to "Confirm download?",
        "no_active_downloads" to "No active downloads in progress",
        "error_parsing_failed" to "Could not auto-detect any stream. Real media URL extraction failed.",
        "success_added" to "Added to downloads queue",
        "btn_cancel" to "Cancel",
        "btn_pause" to "Pause",
        "btn_resume" to "Resume",
        "btn_retry" to "Retry",
        "btn_delete" to "Delete",
        "setting_theme" to "Dark Mode",
        "setting_clear" to "Clear Browser History",
        "toast_history_cleared" to "Browser history cleared",
        "media_not_found" to "No downloadable streams found. Please try another URL.",
        "extracting" to "Analyzing webpage streams..."
    )

    private val ar = mapOf(
        "title_browser" to "المتصفح",
        "title_downloads" to "التنزيلات",
        "title_files" to "الملفات المحفوظة",
        "title_settings" to "الإعدادات",
        "placeholder_url" to "ابحث أو أدخل رابط مباشر",
        "btn_download" to "تحميل",
        "tab_all" to "الكل",
        "tab_video" to "الفيديو",
        "tab_audio" to "الصوت",
        "quality_selection" to "اختر الجودة المناسبة",
        "confirm_download" to "تأكيد التحميل؟",
        "no_active_downloads" to "لا توجد تنزيلات نشطة حالياً",
        "error_parsing_failed" to "فشل استخراج رابط بث الوسائط الحقيقي من هذا الرابط.",
        "success_added" to "تمت الإضافة لقائمة التنزيلات",
        "btn_cancel" to "إلغاء",
        "btn_pause" to "إيقاف مؤقت",
        "btn_resume" to "استئناف",
        "btn_retry" to "إعادة المحاولة",
        "btn_delete" to "حذف",
        "setting_theme" to "الوضع الداكن",
        "setting_clear" to "مسح سجل التصفح",
        "toast_history_cleared" to "تم مسح سجل المتصفح بنجاح",
        "media_not_found" to "لم يتم العثور على بث مباشر قابل للتحميل.",
        "extracting" to "تحليل البث ومصادر الصفحة..."
    )

    fun getString(key: String, lang: String): String {
        return if (lang == "ar") {
            ar[key] ?: en[key] ?: key
        } else {
            en[key] ?: key
        }
    }
}
