package com.example.telegesth;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.telegesth.R;
import com.example.telegesth.databinding.FragmentResumenBinding;
import com.example.telegesth.Transaccion;
import com.example.telegesth.ServicioAlmacenamiento;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResumenFragment extends Fragment {
    private static final String TAG = "ResumenFragment";

    private FragmentResumenBinding binding;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private Calendar calendar;
    private SimpleDateFormat monthFormat;
    private List<String> mesesDisponibles;
    private int mesActual, añoActual;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentResumenBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        inicializarComponentes();
        configurarSelectorMes();
        cargarDatos();

        return root;
    }

    private void inicializarComponentes() {
        servicioAlmacenamiento = new ServicioAlmacenamiento();
        calendar = Calendar.getInstance();
        monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        mesActual = calendar.get(Calendar.MONTH);
        añoActual = calendar.get(Calendar.YEAR);

        configurarGraficos();
        generarMesesDisponibles();
    }

    private void configurarGraficos() {
        // Configurar gráfico circular
        PieChart pieChart = binding.pieChart;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(50f);

        // Configurar gráfico de barras
        BarChart barChart = binding.barChart;
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void generarMesesDisponibles() {
        mesesDisponibles = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // Agregar últimos 12 meses
        for (int i = 11; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, -i);
            mesesDisponibles.add(monthFormat.format(cal.getTime()));
        }
    }

    private void configurarSelectorMes() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, mesesDisponibles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMes.setAdapter(adapter);

        // Seleccionar mes actual
        String mesActualString = monthFormat.format(new Date());
        int posicion = mesesDisponibles.indexOf(mesActualString);
        if (posicion >= 0) {
            binding.spinnerMes.setSelection(posicion);
        }

        binding.spinnerMes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String mesSeleccionado = mesesDisponibles.get(position);
                actualizarDatosPorMes(mesSeleccionado);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void cargarDatos() {
        String mesActualString = monthFormat.format(new Date());
        actualizarDatosPorMes(mesActualString);
    }

    private void actualizarDatosPorMes(String mesSeleccionado) {
        try {
            Date fechaMes = monthFormat.parse(mesSeleccionado);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaMes);

            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long inicioMes = cal.getTimeInMillis();

            cal.add(Calendar.MONTH, 1);
            long finMes = cal.getTimeInMillis();

            servicioAlmacenamiento.obtenerTransaccionesPorMes(inicioMes, finMes)
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Transaccion> transacciones = new ArrayList<>();
                        queryDocumentSnapshots.forEach(doc -> {
                            Transaccion transaccion = doc.toObject(Transaccion.class);
                            transaccion.setId(doc.getId());
                            transacciones.add(transaccion);
                        });

                        actualizarResumen(transacciones);
                        actualizarGraficos(transacciones);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al cargar transacciones", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error al parsear fecha", e);
        }
    }

    private void actualizarResumen(List<Transaccion> transacciones) {
        double totalIngresos = 0;
        double totalEgresos = 0;

        for (Transaccion transaccion : transacciones) {
            if ("Ingreso".equals(transaccion.getTipo())) {
                totalIngresos += transaccion.getMonto();
            } else {
                totalEgresos += transaccion.getMonto();
            }
        }

        double balance = totalIngresos - totalEgresos;

        binding.tvTotalIngresos.setText(String.format("$%.2f", totalIngresos));
        binding.tvTotalEgresos.setText(String.format("$%.2f", totalEgresos));
        binding.tvBalance.setText(String.format("$%.2f", balance));

        // Cambiar color del balance
        binding.tvBalance.setTextColor(balance >= 0 ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));
    }

    private void actualizarGraficos(List<Transaccion> transacciones) {
        actualizarGraficoCircular(transacciones);
        actualizarGraficoBarras(transacciones);
    }

    private void actualizarGraficoCircular(List<Transaccion> transacciones) {
        double totalIngresos = 0;
        double totalEgresos = 0;

        for (Transaccion transaccion : transacciones) {
            if ("Ingreso".equals(transaccion.getTipo())) {
                totalIngresos += transaccion.getMonto();
            } else {
                totalEgresos += transaccion.getMonto();
            }
        }

        if (totalIngresos == 0 && totalEgresos == 0) {
            binding.pieChart.clear();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        if (totalIngresos > 0) {
            entries.add(new PieEntry((float) totalIngresos, "Ingresos"));
        }
        if (totalEgresos > 0) {
            entries.add(new PieEntry((float) totalEgresos, "Egresos"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Distribución");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        binding.pieChart.setData(data);
        binding.pieChart.invalidate();
    }

    private void actualizarGraficoBarras(List<Transaccion> transacciones) {
        // Agrupar por semanas del mes
        float[] ingresosPorSemana = new float[4];
        float[] egresosPorSemana = new float[4];

        for (Transaccion transaccion : transacciones) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(transaccion.getFecha());
            int semana = (cal.get(Calendar.DAY_OF_MONTH) - 1) / 7;
            semana = Math.min(semana, 3); // Máximo 4 semanas

            if ("Ingreso".equals(transaccion.getTipo())) {
                ingresosPorSemana[semana] += transaccion.getMonto();
            } else {
                egresosPorSemana[semana] += transaccion.getMonto();
            }
        }

        ArrayList<BarEntry> ingresosEntries = new ArrayList<>();
        ArrayList<BarEntry> egresosEntries = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            ingresosEntries.add(new BarEntry(i, ingresosPorSemana[i]));
            egresosEntries.add(new BarEntry(i, egresosPorSemana[i]));
        }

        BarDataSet ingresosDataSet = new BarDataSet(ingresosEntries, "Ingresos");
        ingresosDataSet.setColor(Color.GREEN);

        BarDataSet egresosDataSet = new BarDataSet(egresosEntries, "Egresos");
        egresosDataSet.setColor(Color.RED);

        BarData barData = new BarData(ingresosDataSet, egresosDataSet);
        barData.setBarWidth(0.35f);

        binding.barChart.setData(barData);
        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Sem 1", "Sem 2", "Sem 3", "Sem 4"}));
        binding.barChart.groupBars(0, 0.3f, 0.05f);
        binding.barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}