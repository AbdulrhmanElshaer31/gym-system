package com.gym.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Properties;

@Service
public class AdminService {

    private static final String CONFIG_FILE = "admin-config.properties";
    private String adminPassword = "Elshaer"; // القيمة الافتراضية

    public AdminService() {
        loadPasswordFromFile();
    }

    /**
     * تحميل كلمة المرور من الملف
     */
    private void loadPasswordFromFile() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(CONFIG_FILE)) {
                Properties prop = new Properties();
                prop.load(input);
                String savedPassword = prop.getProperty("admin.password");
                if (savedPassword != null && !savedPassword.trim().isEmpty()) {
                    this.adminPassword = savedPassword;
                    System.out.println("تم تحميل كلمة المرور من الملف");
                }
            } catch (IOException e) {
                System.err.println("خطأ في تحميل كلمة المرور: " + e.getMessage());
            }
        } else {
            // إذا لم يكن الملف موجودًا، نقوم بإنشائه بكلمة المرور الافتراضية
            savePasswordToFile();
        }
    }

    /**
     * حفظ كلمة المرور في الملف
     */
    private void savePasswordToFile() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.setProperty("admin.password", adminPassword);
            prop.store(output, "Admin Configuration - Do not share this file");
            System.out.println("تم حفظ كلمة المرور في الملف");
        } catch (IOException e) {
            System.err.println("خطأ في حفظ كلمة المرور: " + e.getMessage());
        }
    }

    /**
     * التحقق من صحة كلمة المرور
     */
    public boolean verifyPassword(String password) {
        return password != null && password.equals(adminPassword);
    }

    /**
     * تغيير كلمة المرور
     */
    public boolean changePassword(String oldPassword, String newPassword, String confirmPassword) {
        // التحقق من كلمة المرور القديمة
        if (!verifyPassword(oldPassword)) {
            return false;
        }

        // التحقق من تطابق كلمات المرور الجديدة
        if (!newPassword.equals(confirmPassword)) {
            return false;
        }

        // التحقق من أن كلمة المرور الجديدة ليست فارغة
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }

        // التحقق من أن كلمة المرور الجديدة مختلفة عن القديمة
        if (newPassword.equals(oldPassword)) {
            return false;
        }

        // تعديل كلمة المرور وحفظها في الملف
        this.adminPassword = newPassword;
        savePasswordToFile();
        return true;
    }

    /**
     * إعادة تعيين كلمة المرور للقيمة الافتراضية
     */
    public void resetPassword() {
        this.adminPassword = "Elshaer";
        savePasswordToFile();
        System.out.println("تم إعادة تعيين كلمة المرور للقيمة الافتراضية");
    }

    /**
     * الحصول على كلمة المرور الحالية (للاختبار فقط - يجب حذفها في الإنتاج)
     */
    public String getCurrentPassword() {
        return adminPassword;
    }
}