package com.zkry.resources.service;

import com.zkry.common.core.domain.PageResult;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TripHistoryPersistenceService {

    private final CrudResourceService crudResourceService;

    public TripHistoryPersistenceService(CrudResourceService crudResourceService) {
        this.crudResourceService = crudResourceService;
    }

    public PageResult<Map<String, Object>> history(int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        return crudResourceService.list("trip-plans", ResourceSearchCriteria.of(null, null, null, null, null, null, 1,
            pageSize));
    }
}
