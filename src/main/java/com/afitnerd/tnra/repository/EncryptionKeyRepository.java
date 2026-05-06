package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.EncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EncryptionKeyRepository extends JpaRepository<EncryptionKey, Long> {

    Optional<EncryptionKey> findFirstByOrderByIdAsc();
}
