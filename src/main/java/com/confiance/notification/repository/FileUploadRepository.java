package com.confiance.notification.repository;

import com.confiance.notification.entity.FileUpload;
import com.confiance.notification.enums.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    Optional<FileUpload> findByPublicId(String publicId);

    Page<FileUpload> findByUserId(Long userId, Pageable pageable);

    Page<FileUpload> findByFileType(FileType fileType, Pageable pageable);

    Page<FileUpload> findByUserIdAndFileType(Long userId, FileType fileType, Pageable pageable);

    List<FileUpload> findByEntityTypeAndEntityId(String entityType, Long entityId);

    Page<FileUpload> findByFolder(String folder, Pageable pageable);

    void deleteByPublicId(String publicId);
}
