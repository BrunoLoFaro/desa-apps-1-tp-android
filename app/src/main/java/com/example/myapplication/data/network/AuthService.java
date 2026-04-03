package com.example.myapplication.data.network;

import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpCodeVerificationRequest;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpRegistrationCompleteRequest;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.model.PasswordResetConfirmRequest;
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
    Call<OtpResponse> requestSignupOtp(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpResponse> resendSignupOtp(@Url String url, @Body OtpRequest request);

    @POST
    Call<LoginResponse> completeSignupWithOtp(@Url String url, @Body OtpRegistrationCompleteRequest request);

    @POST
    Call<OtpResponse> requestPasswordReset(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpResponse> resendPasswordReset(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpResponse> verifyPasswordResetCode(@Url String url, @Body OtpCodeVerificationRequest request);

    @POST
    Call<LoginResponse> confirmPasswordReset(@Url String url, @Body PasswordResetConfirmRequest request);
}
