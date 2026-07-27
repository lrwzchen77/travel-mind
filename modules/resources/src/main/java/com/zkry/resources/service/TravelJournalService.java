package com.zkry.resources.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.exception.BizException;
import com.zkry.resources.domain.JournalLocation;
import com.zkry.resources.domain.JournalPhoto;
import com.zkry.resources.domain.TravelJournal;
import com.zkry.resources.mapper.JournalLocationMapper;
import com.zkry.resources.mapper.JournalPhotoMapper;
import com.zkry.resources.mapper.TravelJournalMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 旅行游记/足迹服务。
 */
@Service
public class TravelJournalService {

    private static final Set<String> VALID_STATUS = Set.of("draft", "published", "archived");
    private static final Set<String> VALID_VISIBILITY = Set.of("private", "public", "friends");

    private final TravelJournalMapper travelJournalMapper;
    private final JournalPhotoMapper journalPhotoMapper;
    private final JournalLocationMapper journalLocationMapper;

    public TravelJournalService(
        TravelJournalMapper travelJournalMapper,
        JournalPhotoMapper journalPhotoMapper,
        JournalLocationMapper journalLocationMapper
    ) {
        this.travelJournalMapper = travelJournalMapper;
        this.journalPhotoMapper = journalPhotoMapper;
        this.journalLocationMapper = journalLocationMapper;
    }

    @Transactional
    public TravelJournal create(long userId, TravelJournal journal) {
        journal.setId(nextId());
        journal.setUserId(userId);
        journal.setViewCount(0);
        journal.setLikeCount(0);
        if (journal.getStatus() == null || !VALID_STATUS.contains(journal.getStatus())) {
            journal.setStatus("draft");
        }
        if (journal.getVisibility() == null || !VALID_VISIBILITY.contains(journal.getVisibility())) {
            journal.setVisibility("private");
        }
        travelJournalMapper.insert(journal);
        return journal;
    }

    @Transactional
    public TravelJournal createFromTrip(long userId, long tripPlanId, String title, String destinationCity, Integer travelDays) {
        TravelJournal existing = travelJournalMapper.selectOne(new LambdaQueryWrapper<TravelJournal>()
            .eq(TravelJournal::getUserId, userId)
            .eq(TravelJournal::getTripPlanId, tripPlanId)
            .eq(TravelJournal::getDeleted, 0));
        if (existing != null) return existing;
        TravelJournal journal = new TravelJournal();
        journal.setId(nextId());
        journal.setUserId(userId);
        journal.setTripPlanId(tripPlanId);
        journal.setTitle(title);
        journal.setDestinationCity(destinationCity);
        journal.setTravelDays(travelDays == null ? 1 : travelDays);
        journal.setStatus("draft");
        journal.setVisibility("private");
        journal.setViewCount(0);
        journal.setLikeCount(0);
        travelJournalMapper.insert(journal);
        return journal;
    }

    public PageResult<TravelJournal> list(long userId, int pageNum, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int page = Math.max(pageNum, 1);
        LambdaQueryWrapper<TravelJournal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelJournal::getUserId, userId)
            .eq(TravelJournal::getDeleted, 0)
            .orderByDesc(TravelJournal::getUpdateTime)
            .orderByDesc(TravelJournal::getId);
        Page<TravelJournal> mpPage = travelJournalMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(mpPage.getRecords(), mpPage.getTotal(), page, size);
    }

    public TravelJournal detail(long userId, long journalId) {
        TravelJournal journal = ownedJournal(userId, journalId);
        journal.setViewCount(journal.getViewCount() + 1);
        travelJournalMapper.updateById(journal);
        return journal;
    }

    public Map<String, Object> detailWithItems(long userId, long journalId) {
        TravelJournal journal = detail(userId, journalId);
        List<JournalPhoto> photos = journalPhotoMapper.selectList(
            new LambdaQueryWrapper<JournalPhoto>()
                .eq(JournalPhoto::getJournalId, journalId)
                .orderByAsc(JournalPhoto::getDayIndex)
                .orderByAsc(JournalPhoto::getSortOrder));
        List<JournalLocation> locations = journalLocationMapper.selectList(
            new LambdaQueryWrapper<JournalLocation>()
                .eq(JournalLocation::getJournalId, journalId)
                .orderByAsc(JournalLocation::getDayIndex)
                .orderByAsc(JournalLocation::getSortOrder));
        return Map.of(
            "journal", journal,
            "photos", photos,
            "locations", locations
        );
    }

    @Transactional
    public TravelJournal update(long userId, long journalId, TravelJournal update) {
        TravelJournal journal = ownedJournal(userId, journalId);
        update.setId(journalId);
        update.setUserId(null);
        update.setCreateTime(null);
        update.setUpdateTime(LocalDateTime.now());
        travelJournalMapper.updateById(update);
        return travelJournalMapper.selectById(journalId);
    }

    @Transactional
    public void delete(long userId, long journalId) {
        TravelJournal journal = ownedJournal(userId, journalId);
        journal.setTripPlanId(null);
        journal.setDeleted(1);
        travelJournalMapper.updateById(journal);
    }

    @Transactional
    public TravelJournal publish(long userId, long journalId) {
        TravelJournal journal = ownedJournal(userId, journalId);
        journal.setStatus("published");
        journal.setVisibility("public");
        travelJournalMapper.updateById(journal);
        return journal;
    }

    @Transactional
    public JournalPhoto addPhoto(long userId, long journalId, JournalPhoto photo) {
        ownedJournal(userId, journalId);
        photo.setId(nextId());
        photo.setJournalId(journalId);
        if (photo.getSortOrder() == null) {
            Long count = journalPhotoMapper.selectCount(
                new LambdaQueryWrapper<JournalPhoto>().eq(JournalPhoto::getJournalId, journalId));
            photo.setSortOrder(count.intValue() + 1);
        }
        journalPhotoMapper.insert(photo);
        return photo;
    }

    @Transactional
    public void deletePhoto(long userId, long journalId, long photoId) {
        ownedJournal(userId, journalId);
        journalPhotoMapper.delete(
            new LambdaQueryWrapper<JournalPhoto>()
                .eq(JournalPhoto::getId, photoId)
                .eq(JournalPhoto::getJournalId, journalId));
    }

    @Transactional
    public JournalLocation addLocation(long userId, long journalId, JournalLocation location) {
        ownedJournal(userId, journalId);
        location.setId(nextId());
        location.setJournalId(journalId);
        if (location.getSortOrder() == null) {
            Long count = journalLocationMapper.selectCount(
                new LambdaQueryWrapper<JournalLocation>().eq(JournalLocation::getJournalId, journalId));
            location.setSortOrder(count.intValue() + 1);
        }
        journalLocationMapper.insert(location);
        return location;
    }

    @Transactional
    public void deleteLocation(long userId, long journalId, long locationId) {
        ownedJournal(userId, journalId);
        journalLocationMapper.delete(
            new LambdaQueryWrapper<JournalLocation>()
                .eq(JournalLocation::getId, locationId)
                .eq(JournalLocation::getJournalId, journalId));
    }

    private TravelJournal ownedJournal(long userId, long journalId) {
        TravelJournal journal = travelJournalMapper.selectById(journalId);
        if (journal == null || journal.getDeleted() == 1 || !journal.getUserId().equals(userId)) {
            throw new BizException("游记不存在或无权访问。");
        }
        return journal;
    }

    private long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
