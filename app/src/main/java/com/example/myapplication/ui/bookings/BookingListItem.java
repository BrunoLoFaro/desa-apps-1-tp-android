package com.example.myapplication.ui.bookings;

import com.example.myapplication.data.model.BookingResponse;

public abstract class BookingListItem {

    public static final class SectionHeader extends BookingListItem {
        public final String title;
        public final boolean isToday;

        public SectionHeader(String title, boolean isToday) {
            this.title = title;
            this.isToday = isToday;
        }
    }

    public static final class BookingItem extends BookingListItem {
        public final BookingResponse booking;
        public final String dayNumber;
        public final String monthAbbr;
        public final boolean isToday;

        public BookingItem(BookingResponse booking, String dayNumber, String monthAbbr, boolean isToday) {
            this.booking = booking;
            this.dayNumber = dayNumber;
            this.monthAbbr = monthAbbr;
            this.isToday = isToday;
        }
    }
}
