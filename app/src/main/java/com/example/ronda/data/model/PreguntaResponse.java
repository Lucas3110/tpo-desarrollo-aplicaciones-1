package com.example.ronda.data.model;
import com.google.gson.annotations.SerializedName;
public class PreguntaResponse {
    @SerializedName("id") private int id;
    @SerializedName("texto") private String texto;
    @SerializedName("respuesta") private String respuesta;
    @SerializedName("respondida") private boolean respondida;
    @SerializedName("respondidaEn") private String respondidaEn;
    @SerializedName("creadoEn") private String creadoEn;
    @SerializedName("autor") private AutorResponse autor;

    public int getId() { return id; }
    public String getTexto() { return texto; }
    public String getRespuesta() { return respuesta; }
    public boolean isRespondida() { return respondida; }
    public String getRespondidaEn() { return respondidaEn; }
    public String getCreadoEn() { return creadoEn; }
    public AutorResponse getAutor() { return autor; }
}