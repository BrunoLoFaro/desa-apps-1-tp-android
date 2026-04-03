package com.example.myapplication.data.network;

import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.model.OtpVerificationRequest;
import com.example.myapplication.data.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface AuthService {
    @POST
    Call<LoginResponse> login(@Url String url, @Body LoginRequest request);

    @POST
    Call<LoginResponse> register(@Url String url, @Body RegisterRequest request);

    @POST
    Call<OtpResponse> requestOtp(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpResponse> resendOtp(@Url String url, @Body OtpRequest request);

    @POST
    Call<LoginResponse> verifyOtp(@Url String url, @Body OtpVerificationRequest request);
}
