package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.resources.domain.TravelJournal;
import com.zkry.resources.mapper.JournalLocationMapper;
import com.zkry.resources.mapper.JournalPhotoMapper;
import com.zkry.resources.mapper.TravelJournalMapper;
import org.junit.jupiter.api.Test;

class TravelJournalServiceTest {

    @Test
    void creatingFromTheSameTripReturnsTheExistingJournal() {
        TravelJournalMapper journals = mock(TravelJournalMapper.class);
        TravelJournal existing = new TravelJournal();
        existing.setId(7L);
        when(journals.selectOne(any())).thenReturn(existing);
        TravelJournalService service = new TravelJournalService(
            journals, mock(JournalPhotoMapper.class), mock(JournalLocationMapper.class));

        TravelJournal result = service.createFromTrip(1001L, 9L, "杭州游记", "杭州", 3);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void publishingMakesTheOwnedJournalPublic() {
        TravelJournalMapper journals = mock(TravelJournalMapper.class);
        TravelJournal journal = new TravelJournal();
        journal.setId(7L);
        journal.setUserId(1001L);
        journal.setDeleted(0);
        journal.setStatus("draft");
        journal.setVisibility("private");
        when(journals.selectById(7L)).thenReturn(journal);
        TravelJournalService service = new TravelJournalService(
            journals, mock(JournalPhotoMapper.class), mock(JournalLocationMapper.class));

        TravelJournal published = service.publish(1001L, 7L);

        assertThat(published.getStatus()).isEqualTo("published");
        assertThat(published.getVisibility()).isEqualTo("public");
        verify(journals).updateById(journal);
    }
}
