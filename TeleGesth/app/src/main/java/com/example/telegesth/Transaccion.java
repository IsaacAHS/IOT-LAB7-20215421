package com.example.telegesth;

import java.util.Date;

public class Transaccion {
    private String id;
    private String titulo;
    private double monto;
    private String descripcion;
    private String tipo;
    private Date fecha;
    private String foto; // Nueva campo para URL de Cloudinary
    private Date fechaCreacion;

    // Constructor vacío requerido por Firestore
    public Transaccion() {}

    public Transaccion(String titulo, double monto, String descripcion, String tipo, Date fecha, String foto) {
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.fecha = fecha;
        this.foto = foto;
        this.fechaCreacion = new Date();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    // Método de compatibilidad para código existente
    public String getImagenNombre() {
        return foto; // Retorna la URL en lugar del nombre del archivo
    }
}