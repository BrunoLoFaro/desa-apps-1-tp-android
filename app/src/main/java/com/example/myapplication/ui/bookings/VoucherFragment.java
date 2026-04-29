package com.example.myapplication.ui.bookings;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.myapplication.R;
import com.example.myapplication.data.local.OfflineBookingEntity;
import com.example.myapplication.util.FormatUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.IOException;
import java.io.OutputStream;

@AndroidEntryPoint
public class VoucherFragment extends Fragment {

    private VoucherViewModel viewModel;
    private OfflineBookingEntity currentBooking;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.voucher_toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        MaterialButton downloadButton = view.findViewById(R.id.voucher_download_button);
        downloadButton.setOnClickListener(v -> generatePdf());

        viewModel = new ViewModelProvider(this).get(VoucherViewModel.class);
        viewModel.getBooking().observe(getViewLifecycleOwner(), booking -> {
            if (booking != null) {
                currentBooking = booking;
                bindData(view, booking);
            }
        });
    }

    private void bindData(View view, OfflineBookingEntity b) {
        setText(view, R.id.voucher_activity_name, b.activityName);
        setText(view, R.id.voucher_destination, b.destinationName);
        setText(view, R.id.voucher_datetime, FormatUtils.formatStartTime(b.sessionStartTime));
        setText(view, R.id.voucher_duration, FormatUtils.formatDuration(b.durationMinutes));
        setText(view, R.id.voucher_participants,
                requireContext().getString(R.string.voucher_participants, b.participants));
        setText(view, R.id.voucher_price, FormatUtils.formatPrice(b.totalPrice, b.currency));

        View guideRow = view.findViewById(R.id.voucher_guide_row);
        if (b.guideName != null && !b.guideName.isEmpty()) {
            setText(view, R.id.voucher_guide, b.guideName);
            if (guideRow != null) guideRow.setVisibility(View.VISIBLE);
        } else {
            if (guideRow != null) guideRow.setVisibility(View.GONE);
        }

        View meetingRow = view.findViewById(R.id.voucher_meeting_point_row);
        if (b.meetingPoint != null && !b.meetingPoint.isEmpty()) {
            setText(view, R.id.voucher_meeting_point, b.meetingPoint);
            if (meetingRow != null) meetingRow.setVisibility(View.VISIBLE);
        } else {
            if (meetingRow != null) meetingRow.setVisibility(View.GONE);
        }

        setText(view, R.id.voucher_code, b.voucherCode != null ? b.voucherCode : "—");
    }

    private void generatePdf() {
        if (currentBooking == null) return;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawPdf(canvas, currentBooking);
        document.finishPage(page);

        String fileName = "voucher_" + (currentBooking.voucherCode != null
                ? currentBooking.voucherCode : currentBooking.id) + ".pdf";

        try {
            Uri uri = savePdf(document, fileName);
            document.close();
            if (uri != null) {
                Toast.makeText(requireContext(), R.string.voucher_pdf_saved, Toast.LENGTH_LONG).show();
                openPdf(uri);
            } else {
                Toast.makeText(requireContext(), R.string.voucher_pdf_error, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            document.close();
            Toast.makeText(requireContext(), R.string.voucher_pdf_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void drawPdf(Canvas canvas, OfflineBookingEntity b) {
        int margin = 48;
        int y = 60;

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#1A1A1A"));
        titlePaint.setTextSize(22f);
        titlePaint.setFakeBoldText(true);

        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#757575"));
        labelPaint.setTextSize(11f);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#1A1A1A"));
        valuePaint.setTextSize(14f);
        valuePaint.setFakeBoldText(true);

        Paint codePaint = new Paint();
        codePaint.setColor(Color.parseColor("#7C3AED"));
        codePaint.setTextSize(24f);
        codePaint.setFakeBoldText(true);

        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#E0E0E0"));
        dividerPaint.setStrokeWidth(1f);

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#1B5E20"));
        headerPaint.setTextSize(16f);
        headerPaint.setFakeBoldText(true);

        // Header
        canvas.drawText("XploreNow", margin, y, headerPaint);
        y += 10;
        canvas.drawLine(margin, y, 595 - margin, y, dividerPaint);
        y += 30;

        // Activity name
        canvas.drawText(safe(b.activityName), margin, y, titlePaint);
        y += 24;

        if (b.destinationName != null) {
            canvas.drawText(b.destinationName, margin, y, labelPaint);
            y += 30;
        }

        canvas.drawLine(margin, y, 595 - margin, y, dividerPaint);
        y += 24;

        // Fields
        y = drawField(canvas, margin, y, "Fecha y hora",
                FormatUtils.formatStartTime(b.sessionStartTime), labelPaint, valuePaint);
        if (b.meetingPoint != null && !b.meetingPoint.isEmpty()) {
            y = drawField(canvas, margin, y, "Punto de encuentro", b.meetingPoint, labelPaint, valuePaint);
        }
        y = drawField(canvas, margin, y, "Duración",
                FormatUtils.formatDuration(b.durationMinutes), labelPaint, valuePaint);
        if (b.guideName != null && !b.guideName.isEmpty()) {
            y = drawField(canvas, margin, y, "Guía", b.guideName, labelPaint, valuePaint);
        }
        y = drawField(canvas, margin, y, "Participantes",
                b.participants + " participante(s)", labelPaint, valuePaint);
        y = drawField(canvas, margin, y, "Precio total",
                FormatUtils.formatPrice(b.totalPrice, b.currency), labelPaint, valuePaint);

        y += 10;
        canvas.drawLine(margin, y, 595 - margin, y, dividerPaint);
        y += 30;

        // Code
        Paint codeLabel = new Paint();
        codeLabel.setColor(Color.parseColor("#757575"));
        codeLabel.setTextSize(11f);
        canvas.drawText("CÓDIGO DE RESERVA", margin, y, codeLabel);
        y += 24;
        canvas.drawText(safe(b.voucherCode), margin, y, codePaint);
        y += 16;

        Paint hintPaint = new Paint();
        hintPaint.setColor(Color.parseColor("#9E9E9E"));
        hintPaint.setTextSize(11f);
        canvas.drawText("Mostrá este código el día de la actividad", margin, y, hintPaint);
    }

    private int drawField(Canvas c, int x, int y, String label, String value,
                          Paint labelPaint, Paint valuePaint) {
        c.drawText(label, x, y, labelPaint);
        c.drawText(value != null ? value : "—", x, y + 18, valuePaint);
        return y + 46;
    }

    @Nullable
    private Uri savePdf(PdfDocument document, String fileName) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = requireContext().getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;
            try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                document.writeTo(out);
            }
            return uri;
        } else {
            java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            java.io.File file = new java.io.File(dir, fileName);
            try (OutputStream out = new java.io.FileOutputStream(file)) {
                document.writeTo(out);
            }
            return Uri.fromFile(file);
        }
    }

    private void openPdf(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text != null ? text : "");
    }

    private String safe(String s) {
        return s != null ? s : "—";
    }
}
