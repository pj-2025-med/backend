package com.example.med.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component
public class DicomUtil {

    public void dicomDeal(FileSystemResource fileSystemResource, HttpServletRequest request, HttpServletResponse response) throws IOException {
        long fileLength = fileSystemResource.contentLength();
        String range = request.getHeader("Range");

        long start = 0;
        long end = fileLength - 1;

        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.substring(6).split("-", 2);
            if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) end = Math.min(Long.parseLong(parts[1]), end);
            // 원하는 청크 제한(예: 1MiB)을 걸고 싶다면:
            long maxChunk = 1024L * 1024L;
            end = Math.min(end, start + maxChunk - 1);

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        } else {
            response.setStatus(HttpServletResponse.SC_OK); // 200 전체
        }

        long contentLength = end - start + 1;
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Length", String.valueOf(contentLength));

        try (InputStream in = fileSystemResource.getInputStream();
             OutputStream out = response.getOutputStream()) {

            in.skip(start);
            byte[] buf = new byte[8192];
            long remaining = contentLength;
            int read;
            while (remaining > 0 && (read = in.read(buf, 0, (int)Math.min(buf.length, remaining))) != -1) {
                out.write(buf, 0, read);
                remaining -= read;
            }
        }
    }

    public String joinWindowsPath(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) continue;
            String p = part.replace('/', '\\');
            if (sb.isEmpty()) {
                sb.append(p);
            } else {
                boolean endBS = sb.charAt(sb.length() - 1) == '\\';
                boolean startBS = p.charAt(0) == '\\';
                if (endBS && startBS) sb.setLength(sb.length() - 1);
                else if (!endBS && !startBS) sb.append('\\');
                sb.append(p);
            }
        }
        return sb.toString();
    }

}
