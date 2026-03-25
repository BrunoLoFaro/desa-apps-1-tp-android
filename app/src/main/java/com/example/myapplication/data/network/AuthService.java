package com.example.myapplication.data.network;

import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpRequestResponse;
import com.example.myapplication.data.model.OtpVerifyRequest;
import com.example.myapplication.data.model.OtpVerifyResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface AuthService {
    @POST
    Call<LoginResponse> login(@Url String url, @Body LoginRequest request);

    @POST
    Call<OtpRequestResponse> requestOtp(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpVerifyResponse> verifyOtp(@Url String url, @Body OtpVerifyRequest request);
}
