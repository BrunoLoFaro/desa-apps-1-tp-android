package com.example.myapplication.ui.checkin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.util.CheckInStore;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Check-in por QR (Feature 11). El guía muestra un código QR en el punto de
 * encuentro; el viajero lo escanea con la cámara y la app confirma su asistencia.
 *
 * <p>Basado en la implementación de referencia {@code QrScanFragment} de
 * mobile-practices-android (CameraX + ML Kit), adaptado al dominio de XploreNow:
 * el contenido del QR se valida contra la reserva actual y se muestra un
 * resultado visual claro (verde = confirmado, rojo = QR inválido).</p>
 */
@AndroidEntryPoint
public class CheckInScanFragment extends Fragment {

    private static final String TAG = "CheckInScanFragment";

    private static final int COLOR_SUCCESS = Color.parseColor("#1B5E20");
    private static final int COLOR_ERROR = Color.parseColor("#B71C1C");

    private PreviewView previewView;
    private View hint;
    private LinearLayout resultPanel;
    private ImageView ivResultIcon;
    private TextView tvResultTitle;
    private TextView tvResultMessage;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Datos de la reserva con la que validamos el QR escaneado.
    private long bookingId;
    private String expectedVoucherCode;
    private long expectedSessionId;
    private String activityName;

    // Evita procesar múltiples frames una vez que ya mostramos un resultado.
    private final AtomicBoolean handled = new AtomicBoolean(false);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            bookingId = getArguments().getLong("bookingId", -1L);
            expectedVoucherCode = getArguments().getString("voucherCode");
            expectedSessionId = getArguments().getLong("sessionId", -1L);
            activityName = getArguments().getString("activityName");
        }

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        iniciarCamara();
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.checkin_permission_denied, Toast.LENGTH_LONG).show();
                    }
                });

        BarcodeScannerOptions opciones = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(opciones);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_check_in_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        hint = view.findViewById(R.id.tvHint);
        resultPanel = view.findViewById(R.id.resultPanel);
        ivResultIcon = view.findViewById(R.id.ivResultIcon);
        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultMessage = view.findViewById(R.id.tvResultMessage);

        view.findViewById(R.id.btnScanAgain).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        pedirPermisoCamara();
    }

    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> futuro =
                ProcessCameraProvider.getInstance(requireContext());

        futuro.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = futuro.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analisis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analisis.setAnalyzer(cameraExecutor, this::analizarFrame);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error al obtener ProcessCameraProvider", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void analizarFrame(@NonNull ImageProxy imageProxy) {
        if (handled.get() || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage imagen = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(imagen)
                .addOnSuccessListener(codigos -> {
                    for (Barcode codigo : codigos) {
                        String valor = codigo.getRawValue();
                        if (valor != null && handled.compareAndSet(false, true)) {
                            requireActivity().runOnUiThread(() -> mostrarResultado(valor));
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error en ML Kit barcode scan", e))
                .addOnCompleteListener(tarea -> imageProxy.close());
    }

    /** Valida el contenido del QR contra la reserva y muestra el resultado verde/rojo. */
    private void mostrarResultado(String rawValue) {
        if (!isAdded()) return;
        boolean valido = esQrDeLaReserva(rawValue);

        hint.setVisibility(View.GONE);
        resultPanel.setVisibility(View.VISIBLE);

        if (valido) {
            if (bookingId > 0) {
                CheckInStore.markConfirmed(requireContext(), bookingId);
            }
            resultPanel.setBackgroundColor(COLOR_SUCCESS);
            ivResultIcon.setImageResource(R.drawable.ic_check_circle);
            tvResultTitle.setText(R.string.checkin_success_title);
            tvResultMessage.setText(getString(R.string.checkin_success_message,
                    activityName != null ? activityName : ""));
        } else {
            resultPanel.setBackgroundColor(COLOR_ERROR);
            ivResultIcon.setImageResource(R.drawable.ic_error_circle);
            tvResultTitle.setText(R.string.checkin_error_title);
            tvResultMessage.setText(R.string.checkin_error_message);
        }
    }

    /**
     * El QR del guía es válido si su contenido coincide con la reserva actual:
     * puede ser el código de voucher en texto plano, o un JSON que contenga
     * {@code voucherCode} o {@code sessionId}.
     */
    private boolean esQrDeLaReserva(String rawValue) {
        String valor = rawValue.trim();

        // 1) JSON con voucherCode / sessionId
        try {
            JSONObject json = new JSONObject(valor);
            if (expectedVoucherCode != null && json.has("voucherCode")
                    && expectedVoucherCode.equalsIgnoreCase(json.optString("voucherCode").trim())) {
                return true;
            }
            if (expectedSessionId > 0 && json.has("sessionId")
                    && expectedSessionId == json.optLong("sessionId", -1L)) {
                return true;
            }
            return false;
        } catch (JSONException ignored) {
            // No es JSON: lo tratamos como código de voucher en texto plano.
        }

        // 2) Texto plano == código de voucher
        return expectedVoucherCode != null && expectedVoucherCode.equalsIgnoreCase(valor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}
