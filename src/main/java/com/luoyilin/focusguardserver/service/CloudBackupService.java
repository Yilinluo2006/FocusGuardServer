package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.dto.*;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.CloudBackupRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service
public class CloudBackupService {
    private final CloudBackupRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();
    public CloudBackupService(CloudBackupRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public List<BackupResponse> list(User user) {
        requireUser(user);
        return repository.findAllByUserOrderByUpdatedAtDesc(user).stream().map(value -> BackupResponse.from(value, false)).toList();
    }
    @Transactional(readOnly = true)
    public BackupResponse get(User user, String device) {
        requireUser(user); requireDevice(device);
        return BackupResponse.from(repository.findByUserAndDeviceId(user, device)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "尚无这台设备的备份")), true);
    }
    @Transactional
    public BackupResponse put(User user, String device, BackupRequest request) {
        requireUser(user); requireDevice(device); validatePayload(request.payload());
        repository.lockOwner(user.getId()).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "登录用户不存在"));
        String hash = hash(request.payload());
        CloudBackup value = repository.findByUserAndDeviceId(user, device).orElse(null);
        // Retries after a lost response acknowledge the exact same snapshot without another revision.
        if (value != null && hash.equals(value.getHash())) return BackupResponse.from(value, false);
        long revision = value == null ? 0 : value.getRevision();
        if (request.expectedRevision() == null || request.expectedRevision() != revision)
            throw error(HttpStatus.CONFLICT, "云端备份版本已变化，请重新核对");
        if (value == null) value = new CloudBackup(user, device);
        value.update(request.deviceName(), request.payload(), hash);
        return BackupResponse.from(repository.saveAndFlush(value), false);
    }
    private void validatePayload(String payload) {
        if (payload == null || payload.getBytes(StandardCharsets.UTF_8).length > 4_000_000)
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "备份超过首版大小限制");
        try {
            var root = mapper.readTree(payload);
            if (root == null || !root.isObject() || root.path("schemaVersion").asInt() != 1
                    || !root.path("limits").isArray() || !root.path("tasks").isArray() || !root.path("focus").isObject())
                throw error(HttpStatus.BAD_REQUEST, "备份格式或版本不支持");
        } catch (ResponseStatusException error) { throw error; }
        catch (Exception error) { throw error(HttpStatus.BAD_REQUEST, "备份内容不是有效 JSON"); }
    }
    private static void requireUser(User user) {
        if (user == null || user.getId() == null) throw error(HttpStatus.UNAUTHORIZED, "请先登录");
    }
    private static void requireDevice(String device) {
        try { if (!UUID.fromString(device).toString().equals(device)) throw new IllegalArgumentException(); }
        catch (RuntimeException invalid) { throw error(HttpStatus.BAD_REQUEST, "设备编号无效"); }
    }
    private static String hash(String payload) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
