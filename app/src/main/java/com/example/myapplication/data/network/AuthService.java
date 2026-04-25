package com.example.myapplication.data.network;

import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpCodeVerificationRequest;
import com.example.myapplication.data.model.OtpRequest;
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

    /** POST /auth/register — crea usuario pendiente y envía OTP al email. */
    @POST
    Call<OtpResponse> register(@Url String url, @Body RegisterRequest request);

    // ── OTP signup (verificación tras registro) ───────────────────────────────
    @POST
    Call<OtpResponse> resendSignupOtp(@Url String url, @Body OtpRequest request);

    /** Verifica OTP y activa usuario; devuelve sesión. */
    @POST
    Call<LoginResponse> verifySignupOtp(@Url String url, @Body OtpCodeVerificationRequest request);

    // ── OTP login (ingresar con código de un solo uso) ────────────────────────
    @POST
    Call<OtpResponse> sendLoginOtp(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpResponse> resendLoginOtp(@Url String url, @Body OtpRequest request);

    @POST
    Call<LoginResponse> verifyLoginOtp(@Url String url, @Body OtpCodeVerificationRequest request);

    // ── Password reset ────────────────────────────────────────────────────────
    @POST
    Call<OtpResponse> requestPasswordReset(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpResponse> resendPasswordReset(@Url String url, @Body OtpRequest request);

    @POST
    Call<OtpResponse> verifyPasswordResetCode(@Url String url, @Body OtpCodeVerificationRequest request);

    @POST
    Call<LoginResponse> confirmPasswordReset(@Url String url, @Body PasswordResetConfirmRequest request);
}