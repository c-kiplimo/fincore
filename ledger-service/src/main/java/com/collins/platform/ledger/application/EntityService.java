package com.collins.platform.ledger.application;

import com.collins.platform.commons.exception.ConflictException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.ledger.infrastructure.LedgerRepository;
import com.collins.platform.ledger.infrastructure.LedgerRows.ChartAccount;
import com.collins.platform.ledger.infrastructure.LedgerRows.EntityRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EntityService {

    private final LedgerRepository repo;

    public EntityService(LedgerRepository repo) {
        this.repo = repo;
    }

    public List<EntityRow> list() {
        return repo.entities();
    }

    public EntityRow get(String entityId) {
        return repo.entity(entityId).orElseThrow(() -> new NotFoundException("Legal entity", entityId));
    }

    /** Opens a new legal entity with a copy of an existing entity's chart of accounts. */
    @Transactional
    public EntityRow create(EntityRow entity, String copyChartFrom) {
        if (repo.entity(entity.entityId()).isPresent()) {
            throw new ConflictException("ENTITY_EXISTS", "Entity " + entity.entityId() + " already exists");
        }
        get(copyChartFrom);
        repo.insertEntity(entity, copyChartFrom);
        return entity;
    }

    public List<ChartAccount> chart(String entityId) {
        get(entityId);
        return repo.chart(entityId);
    }
}
