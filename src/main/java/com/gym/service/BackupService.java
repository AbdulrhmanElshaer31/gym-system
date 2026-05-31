package com.gym.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Create a backup using SQL SCRIPT command
     */
    public String createBackup(String backupDir) {
        try {
            // إنشاء مجلد النسخ الاحتياطي
            File dir = new File(backupDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            String sqlFileName = "gymdb_backup_" + timestamp + ".sql";
            String sqlFilePath = backupDir + "/" + sqlFileName;
            String zipFilePath = backupDir + "/gymdb_backup_" + timestamp + ".zip";

            System.out.println("=== بدء عملية النسخ الاحتياطي ===");

            // خطوة 1: إنشاء SQL dump باستخدام SCRIPT command
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {

                System.out.println("جاري إنشاء SQL dump...");

                // عمل CHECKPOINT أولاً
                stmt.execute("CHECKPOINT SYNC");

                // إنشاء SQL script
                String scriptCommand = "SCRIPT TO '" + sqlFilePath.replace("\\", "\\\\") + "'";
                System.out.println("تنفيذ: " + scriptCommand);
                stmt.execute(scriptCommand);

                System.out.println("✓ تم إنشاء SQL dump بنجاح");
            }

            // خطوة 2: التحقق من إنشاء الملف
            File sqlFile = new File(sqlFilePath);
            if (!sqlFile.exists() || sqlFile.length() == 0) {
                throw new RuntimeException("فشل إنشاء ملف SQL dump");
            }

            System.out.println("حجم ملف SQL: " + (sqlFile.length() / 1024) + " KB");

            // خطوة 3: ضغط الملف في ZIP
            System.out.println("جاري ضغط الملف...");
            try (FileOutputStream fos = new FileOutputStream(zipFilePath);
                 ZipOutputStream zos = new ZipOutputStream(fos);
                 FileInputStream fis = new FileInputStream(sqlFile)) {

                ZipEntry zipEntry = new ZipEntry(sqlFileName);
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[8192];
                int length;
                long totalBytes = 0;

                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                    totalBytes += length;
                }

                zos.closeEntry();
                System.out.println("✓ تم ضغط الملف: " + (totalBytes / 1024) + " KB");
            }

            // خطوة 4: حذف ملف SQL المؤقت
            sqlFile.delete();

            // خطوة 5: التحقق النهائي
            File zipFile = new File(zipFilePath);
            if (!zipFile.exists() || zipFile.length() == 0) {
                throw new RuntimeException("فشل إنشاء ملف ZIP");
            }

            System.out.println("✓✓✓ النسخة الاحتياطية جاهزة ✓✓✓");
            System.out.println("الموقع: " + zipFilePath);
            System.out.println("الحجم: " + (zipFile.length() / 1024) + " KB");

            return zipFilePath;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("فشل إنشاء النسخة الاحتياطية: " + e.getMessage(), e);
        }
    }

    /**
     * Restore a backup from SQL script
     */
    public void restoreBackup(String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                throw new RuntimeException("ملف النسخة الاحتياطية غير موجود: " + backupFilePath);
            }

            if (backupFile.length() == 0) {
                throw new RuntimeException("ملف النسخة الاحتياطية فارغ!");
            }

            System.out.println("=== بدء استعادة النسخة الاحتياطية ===");
            System.out.println("الملف: " + backupFilePath);
            System.out.println("الحجم: " + (backupFile.length() / 1024) + " KB");

            // خطوة 1: فك ضغط ملف SQL
            String tempDir = System.getProperty("java.io.tmpdir");
            String sqlFilePath = tempDir + "/temp_restore_" + System.currentTimeMillis() + ".sql";

            System.out.println("جاري فك ضغط الملف...");
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                ZipEntry entry = zis.getNextEntry();

                if (entry == null) {
                    throw new RuntimeException("ملف ZIP فارغ أو تالف!");
                }

                try (FileOutputStream fos = new FileOutputStream(sqlFilePath)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    long totalBytes = 0;

                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                        totalBytes += length;
                    }

                    System.out.println("✓ تم فك الضغط: " + (totalBytes / 1024) + " KB");
                }
            }

            File sqlFile = new File(sqlFilePath);
            if (!sqlFile.exists() || sqlFile.length() == 0) {
                throw new RuntimeException("فشل فك ضغط ملف SQL");
            }

            // خطوة 2: حذف البيانات الحالية
            System.out.println("جاري حذف البيانات الحالية...");
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {

                // الحصول على قائمة بجميع الجداول
                ResultSet rs = stmt.executeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'"
                );

                java.util.List<String> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
                rs.close();

                // تعطيل foreign key constraints مؤقتاً
                stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");

                // حذف جميع الجداول
                for (String table : tables) {
                    System.out.println("حذف جدول: " + table);
                    stmt.execute("DROP TABLE IF EXISTS " + table + " CASCADE");
                }

                // إعادة تفعيل foreign key constraints
                stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");

                System.out.println("✓ تم حذف البيانات القديمة");
            }

            // خطوة 3: استعادة البيانات من SQL script
            System.out.println("جاري استعادة البيانات...");
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {

                String runScriptCommand = "RUNSCRIPT FROM '" +
                        sqlFilePath.replace("\\", "\\\\") + "'";

                System.out.println("تنفيذ: " + runScriptCommand);
                stmt.execute(runScriptCommand);

                System.out.println("✓ تم استعادة البيانات بنجاح");
            }

            // خطوة 4: حذف ملف SQL المؤقت
            sqlFile.delete();

            // خطوة 5: عمل CHECKPOINT
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
                stmt.execute("CHECKPOINT SYNC");
            }

            System.out.println("✓✓✓ تم استعادة النسخة الاحتياطية بنجاح ✓✓✓");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("فشل استعادة النسخة الاحتياطية: " + e.getMessage(), e);
        }
    }

    /**
     * الحصول على قائمة بجميع النسخ الاحتياطية المتاحة
     */
    public File[] getAvailableBackups(String backupDir) {
        File dir = new File(backupDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return new File[0];
        }

        File[] backups = dir.listFiles((d, name) ->
                name.startsWith("gymdb_backup_") && name.endsWith(".zip"));

        return backups != null ? backups : new File[0];
    }

    /**
     * حذف النسخ الاحتياطية القديمة
     */
    public void cleanOldBackups(String backupDir, int keepCount) {
        File[] backups = getAvailableBackups(backupDir);

        if (backups.length <= keepCount) {
            return;
        }

        java.util.Arrays.sort(backups, (f1, f2) ->
                Long.compare(f2.lastModified(), f1.lastModified()));

        for (int i = keepCount; i < backups.length; i++) {
            boolean deleted = backups[i].delete();
            System.out.println("حذف نسخة قديمة: " + backups[i].getName() +
                    " - " + (deleted ? "✓" : "✗"));
        }
    }

    /**
     * طباعة معلومات قاعدة البيانات
     */
    public void printDatabaseInfo() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {

            System.out.println("=== معلومات قاعدة البيانات ===");
            System.out.println("Database URL: " + connection.getMetaData().getURL());

            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'"
            );
            if (rs.next()) {
                System.out.println("عدد الجداول: " + rs.getInt(1));
            }
            rs.close();

            // عرض أسماء الجداول
            rs = stmt.executeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'"
            );
            System.out.println("الجداول الموجودة:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString(1));
            }
            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}