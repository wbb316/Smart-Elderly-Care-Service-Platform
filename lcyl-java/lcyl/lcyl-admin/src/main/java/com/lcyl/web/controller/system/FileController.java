package com.lcyl.web.controller.system;

import com.lcyl.common.utils.Result;
import com.lcyl.web.config.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private OssService ossService;
    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};

    // 生成新的文件名
    private String createNewFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename != null ? originalFilename.lastIndexOf('.') : -1;
        String suffix = dotIndex >= 0 ? originalFilename.substring(dotIndex) : ".jpg";
        return UUID.randomUUID() + suffix;
    }
    /**
     * 图片上传
     */
    @PostMapping("upload")
    public Result<String> upload(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择文件！");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return Result.error("文件名不能为空");
            }
            // 白名单校验：仅允许图片格式（取最后一个点后的扩展名，防止 a.exe.jpg 绕过）
            String lowerName = originalFilename.toLowerCase();
            int dotIndex = lowerName.lastIndexOf('.');
            String fileExt = dotIndex >= 0 ? lowerName.substring(dotIndex) : "";
            boolean allowed = false;
            for (String ext : ALLOWED_IMAGE_EXTENSIONS) {
                if (fileExt.equals(ext)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return Result.error("仅支持上传图片格式文件（jpg, jpeg, png, gif, bmp, webp）");
            }
            String newFileName = createNewFileName(file);
            String uploadUrl;
            try (InputStream in = file.getInputStream()) {
                uploadUrl = ossService.upload(in, newFileName);
            }
            return Result.success(uploadUrl);
        }catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("上传文件失败");

        }
    }

//    @PostMapping("/multipleUpload")
//    public Result multipleUpload(MultipartFile[] files) throws IOException {
//        // 判断是否选择文件
//        if (files.length == 0) {
//            return Result.error("请选择文件");
//        }
//        List<String> filePaths = new ArrayList<> ();
//        for (MultipartFile file : files) {
//            InputStream in = file.getInputStream();
//            String newFileName = createNewFileName(file);
//            String uploadUrl = ossService.upload(in, newFileName);
//            filePaths.add(uploadUrl); // 添加到集合中
//        }
//        return Result.success(filePaths);
//    }

    /**
     * 【新增】合同PDF专用上传
     */
    @PostMapping("/uploadContractPdf")
    public Result<String> uploadContractPdf(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择PDF文件！");
            }

            String originalFilename = file.getOriginalFilename();
            // 兼容 Java 8 的空判断
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return Result.error("文件名不能为空");
            }

            // 兼容大小写 .pdf 判断
            if (!originalFilename.toLowerCase().endsWith(".pdf")) {
                return Result.error("仅支持上传PDF格式文件");
            }

            String uploadUrl;
            try (InputStream in = file.getInputStream()) {
                uploadUrl = ossService.uploadContractPdf(in, originalFilename);
            }
            return Result.success(uploadUrl);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("PDF上传失败：" + e.getMessage());
        }
    }


}