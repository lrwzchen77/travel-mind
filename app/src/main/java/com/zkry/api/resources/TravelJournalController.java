package com.zkry.api.resources;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.domain.JournalLocation;
import com.zkry.resources.domain.JournalPhoto;
import com.zkry.resources.domain.TravelJournal;
import com.zkry.resources.service.TravelJournalService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旅行游记/足迹接口。
 */
@RestController
@RequestMapping("/api/user/journals")
public class TravelJournalController {

    private final TravelJournalService travelJournalService;

    public TravelJournalController(TravelJournalService travelJournalService) {
        this.travelJournalService = travelJournalService;
    }

    @GetMapping
    public R<PageResult<TravelJournal>> list(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return R.ok(travelJournalService.list(LoginHelper.getUserId(), pageNum, pageSize));
    }

    @PostMapping
    public R<TravelJournal> create(@RequestBody TravelJournal journal) {
        return R.ok(travelJournalService.create(LoginHelper.getUserId(), journal));
    }

    @GetMapping("/{journalId}")
    public R<Map<String, Object>> detail(@PathVariable long journalId) {
        return R.ok(travelJournalService.detailWithItems(LoginHelper.getUserId(), journalId));
    }

    @PutMapping("/{journalId}")
    public R<TravelJournal> update(@PathVariable long journalId, @RequestBody TravelJournal journal) {
        return R.ok(travelJournalService.update(LoginHelper.getUserId(), journalId, journal));
    }

    @DeleteMapping("/{journalId}")
    public R<Void> delete(@PathVariable long journalId) {
        travelJournalService.delete(LoginHelper.getUserId(), journalId);
        return R.ok();
    }

    @PostMapping("/{journalId}/publish")
    public R<TravelJournal> publish(@PathVariable long journalId) {
        return R.ok(travelJournalService.publish(LoginHelper.getUserId(), journalId));
    }

    @PostMapping("/{journalId}/photos")
    public R<JournalPhoto> addPhoto(@PathVariable long journalId, @RequestBody JournalPhoto photo) {
        return R.ok(travelJournalService.addPhoto(LoginHelper.getUserId(), journalId, photo));
    }

    @DeleteMapping("/{journalId}/photos/{photoId}")
    public R<Void> deletePhoto(@PathVariable long journalId, @PathVariable long photoId) {
        travelJournalService.deletePhoto(LoginHelper.getUserId(), journalId, photoId);
        return R.ok();
    }

    @PostMapping("/{journalId}/locations")
    public R<JournalLocation> addLocation(@PathVariable long journalId, @RequestBody JournalLocation location) {
        return R.ok(travelJournalService.addLocation(LoginHelper.getUserId(), journalId, location));
    }

    @DeleteMapping("/{journalId}/locations/{locationId}")
    public R<Void> deleteLocation(@PathVariable long journalId, @PathVariable long locationId) {
        travelJournalService.deleteLocation(LoginHelper.getUserId(), journalId, locationId);
        return R.ok();
    }
}
