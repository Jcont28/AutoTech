package hn.autotech.service;

import hn.autotech.dao.JdbcUtil;
import hn.autotech.dao.Transaccion;
import hn.autotech.model.Reporte;
import hn.autotech.model.ResultadoConsulta;

public class ReporteService {
    public ResultadoConsulta ejecutar(Reporte reporte) {
        return Transaccion.consulta(c -> JdbcUtil.tabla(c, reporte.sql()));
    }
}
