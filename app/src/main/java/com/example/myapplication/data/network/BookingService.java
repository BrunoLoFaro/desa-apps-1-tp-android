package com.example.myapplication.data.network;

import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingsPageResponse;
import com.example.myapplication.data.model.CreateBookingRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface BookingService {

    @POST
    Call<BookingResponse> createBooking(@Url String url, @Body CreateBookingRequest request);

    @GET
    Call<BookingsPageResponse> listBookings(@Url String url);

    @DELETE
    Call<BookingResponse> cancelBooking(@Url String url);
}

