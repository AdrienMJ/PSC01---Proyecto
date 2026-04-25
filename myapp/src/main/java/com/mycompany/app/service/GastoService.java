package com.mycompany.app.service;

import com.mycompany.app.dto.BalancePersonaDTO;
import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.dto.TransferenciaDTO;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.CategoriaGasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    

    // Añadimos Moneda monedaOrigen como parámetro
    public Gasto crear(Gasto gasto) throws Exception {
        
        
        if (gasto.getMonto() == null || gasto.getMonto() <= 0) {
            throw new Exception("El monto debe ser mayor que 0");
        }

        if (gasto.getGrupo() == null || gasto.getGrupo().getId() == null) {
            throw new Exception("El grupo es obligatorio");
        }

        if (gasto.getPagador() == null || gasto.getPagador().getId() == null) {
            throw new Exception("El pagador es obligatorio");
        }

        
        Grupo grupo = grupoRepository.findById(gasto.getGrupo().getId())
                .orElseThrow(() -> new Exception("Grupo no encontrado"));

        Usuario pagador = usuarioRepository.findById(gasto.getPagador().getId())
                .orElseThrow(() -> new Exception("Pagador no encontrado"));

        
        // Solo convertimos si el gasto viene en una moneda distinta a la del grupo
        if (gasto.getMoneda() != null && grupo.getMoneda() != null) {
            if (!gasto.getMoneda().equals(grupo.getMoneda())) {
                
                double montoConvertido = realizarConversion(gasto.getMonto(), gasto.getMoneda(), grupo.getMoneda());
                
                //Redondeamos a 2 decimales
                double montoFinal = Math.round(montoConvertido * 100.0) / 100.0;
                
                //Seguridad: Evitar que errores de redondeo o de la API devuelvan 0
                if (montoFinal <= 0 && gasto.getMonto() > 0) {
                    throw new Exception("Error en la conversión: el monto resultante es demasiado pequeño o inválido.");
                }

                gasto.setMonto(montoFinal);
                gasto.setMoneda(grupo.getMoneda()); // El gasto queda normalizado a la moneda del grupo
            }
        }

        
        Set<Long> idsMiembros = grupo.getMiembros().stream()
                .map(Usuario::getId)
                .collect(Collectors.toSet());
        
        if (!idsMiembros.contains(pagador.getId())) {
            throw new Exception("El pagador no pertenece al grupo");
        }

        
        if (gasto.getCategoria() == null) {
            gasto.setCategoria(CategoriaGasto.OTROS);
        }

        if (gasto.getEmote() != null && gasto.getEmote().isBlank()) {
            gasto.setEmote(null);
        }

        
        List<Usuario> participantesFinales;
        List<Usuario> participantesRecibidos = gasto.getParticipantes() == null ? List.of() : gasto.getParticipantes();

        if (gasto.isRepartoGeneral() || participantesRecibidos.isEmpty()) {
            gasto.setRepartoGeneral(true);
            participantesFinales = new ArrayList<>(grupo.getMiembros());
        } else {
            Set<Long> idsParticipantes = participantesRecibidos.stream()
                    .map(Usuario::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(HashSet::new));

            if (idsParticipantes.isEmpty()) {
                throw new Exception("Debes seleccionar al menos un participante");
            }

            if (!idsMiembros.containsAll(idsParticipantes)) {
                throw new Exception("Todos los participantes deben pertenecer al grupo");
            }

            participantesFinales = new ArrayList<>(usuarioRepository.findAllById(idsParticipantes));
            gasto.setRepartoGeneral(false);
        }

        if (participantesFinales.isEmpty()) {
            throw new Exception("No hay participantes para repartir el gasto");
        }

        
        gasto.setGrupo(grupo);
        gasto.setPagador(pagador);
        gasto.setParticipantes(participantesFinales);

        return gastoRepository.save(gasto);
    }

    public List<Gasto> listarPorGrupo(Long grupoId, String ordenar, String direccion, String categoria) {
        String propiedad = "fecha";
        if ("monto".equalsIgnoreCase(ordenar)) {
            propiedad = "monto";
        }

        Sort.Direction sentido = "asc".equalsIgnoreCase(direccion) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(sentido, propiedad);

        if (categoria == null || categoria.isBlank() || "TODAS".equalsIgnoreCase(categoria)) {
            return gastoRepository.findByGrupoId(grupoId, sort);
        }

        CategoriaGasto categoriaEnum = CategoriaGasto.valueOf(categoria.toUpperCase());
        return gastoRepository.findByGrupoIdAndCategoria(grupoId, categoriaEnum, sort);
    }

    public Gasto obtenerPorId(Long id) throws Exception {
        return gastoRepository.findById(id)
            .orElseThrow(() -> new Exception("Gasto no encontrado"));
    }

    public Gasto marcarComoPagado(Long gastoId, Long usuarioId) throws Exception {
        Gasto gasto = gastoRepository.findById(gastoId)
            .orElseThrow(() -> new Exception("Gasto no encontrado"));

        if (gasto.isPagado()) {
            throw new Exception("El gasto ya está marcado como pagado");
        }

        if (gasto.getGrupo() == null || gasto.getGrupo().getMiembros().stream().noneMatch(u -> u.getId().equals(usuarioId))) {
            throw new Exception("Usuario no pertenece al grupo");
        }

        if (gasto.getPagador() != null && gasto.getPagador().getId().equals(usuarioId)) {
            throw new Exception("El pagador no puede marcar su propio gasto como pagado");
        }

        gasto.setPagado(true);
        return gastoRepository.save(gasto);
    }

    public ResumenGrupoDTO obtenerResumenGrupo(Long grupoId) throws Exception {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new Exception("Grupo no encontrado"));

        List<Usuario> miembros = grupo.getMiembros();
        List<Gasto> gastos = gastoRepository.findByGrupoId(grupoId);

        // Nuevo: para calcular el total gastado sumando todos los montos válidos
        double totalGastado = gastos.stream().filter(g -> g.getMonto() != null && g.getMonto() > 0)
            .mapToDouble(Gasto::getMonto).sum();
        
        totalGastado = redondear2(totalGastado);

        Map<Long, Double> balances = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        for (Usuario u : miembros) {
            balances.put(u.getId(), 0.0);
            nombres.put(u.getId(), u.getUsername());
        }

        for (Gasto gasto : gastos) {
            if (gasto.isPagado() || gasto.getMonto() == null || gasto.getMonto() <= 0 || gasto.getPagador() == null) {
                continue;
            }

            List<Usuario> involucrados = (gasto.isRepartoGeneral() || gasto.getParticipantes() == null || gasto.getParticipantes().isEmpty())
                    ? miembros
                    : gasto.getParticipantes();

            if (involucrados.isEmpty()) {
                continue;
            }

            double parte = gasto.getMonto() / involucrados.size();
            for (Usuario u : involucrados) {
                if (balances.containsKey(u.getId())) {
                    balances.put(u.getId(), balances.get(u.getId()) - parte);
                }
            }

            if (balances.containsKey(gasto.getPagador().getId())) {
                balances.put(gasto.getPagador().getId(), balances.get(gasto.getPagador().getId()) + gasto.getMonto());
            }
        }

        List<BalancePersonaDTO> balancesDTO = miembros.stream()
                .map(u -> {
                    double b = redondear2(balances.getOrDefault(u.getId(), 0.0));
                    String estado;
                    if (b > 0.009) {
                        estado = "positivo";
                    } else if (b < -0.009) {
                        estado = "debe";
                    } else {
                        estado = "equilibrado";
                    }
                    return new BalancePersonaDTO(u.getId(), u.getUsername(), b, estado);
                })
                .sorted(Comparator.comparing(BalancePersonaDTO::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<BalancePersonaDTO> acreedores = new ArrayList<>(balancesDTO.stream().filter(b -> b.getBalance() > 0.009).toList());
        List<BalancePersonaDTO> deudores = new ArrayList<>(balancesDTO.stream().filter(b -> b.getBalance() < -0.009).toList());

        int i = 0;
        int j = 0;
        List<TransferenciaDTO> transferencias = new ArrayList<>();

        while (i < deudores.size() && j < acreedores.size()) {
            BalancePersonaDTO deudor = deudores.get(i);
            BalancePersonaDTO acreedor = acreedores.get(j);

            double deuda = redondear2(-deudor.getBalance());
            double credito = redondear2(acreedor.getBalance());
            double pago = redondear2(Math.min(deuda, credito));

            if (pago > 0.0) {
                transferencias.add(new TransferenciaDTO(
                        deudor.getUsuarioId(),
                        deudor.getUsername(),
                        acreedor.getUsuarioId(),
                        acreedor.getUsername(),
                        pago
                ));
            }

            double nuevoDeudor = redondear2(deudor.getBalance() + pago);
            double nuevoAcreedor = redondear2(acreedor.getBalance() - pago);

            deudores.set(i, new BalancePersonaDTO(deudor.getUsuarioId(), deudor.getUsername(), nuevoDeudor, nuevoDeudor < -0.009 ? "debe" : "equilibrado"));
            acreedores.set(j, new BalancePersonaDTO(acreedor.getUsuarioId(), acreedor.getUsername(), nuevoAcreedor, nuevoAcreedor > 0.009 ? "positivo" : "equilibrado"));

            if (Math.abs(nuevoDeudor) <= 0.009) {
                i++;
            }
            if (Math.abs(nuevoAcreedor) <= 0.009) {
                j++;
            }
        }

        return new ResumenGrupoDTO(totalGastado, balancesDTO, transferencias);
    }

    private double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public Double calcularTotalGrupo(Long grupoId) {
        List<Gasto> gastos = gastoRepository.findByGrupoId(grupoId);
    
        return gastos.stream().mapToDouble(Gasto::getMonto).sum();
    }

    // Método para convertir la moneda
    @SuppressWarnings("unchecked")
    private double realizarConversion(double monto, Moneda origen, Moneda destino) {
        if (origen == destino || origen == null || destino == null) {
            return monto;
        }

        try {
            String isoOrigen = obtenerCodigoIso(origen);
            String isoDestino = obtenerCodigoIso(destino);

            String url = "https://open.er-api.com/v6/latest/" + isoOrigen;
            RestTemplate restTemplate = new RestTemplate();
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("rates")) {
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                if (rates.containsKey(isoDestino)) {
                    Number tasa = (Number) rates.get(isoDestino);
                    return monto * tasa.doubleValue();
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo el tipo de cambio: " + e.getMessage());
        }
        return monto;
    }

    // Traductor de Enum a ISO
    private String obtenerCodigoIso(Moneda moneda) {
        if (moneda == null) return "EUR";
        switch (moneda) {
            case EURO: return "EUR";
            case DOLAR: return "USD";
            case LIBRA: return "GBP";
            case YEN: return "JPY";
            case PESO: return "MXN"; 
            case FRANCO: return "CHF";
            case CORONA: return "SEK";
            case REAL: return "BRL";
            case RUPIA: return "INR";
            case LIRA: return "TRY";
            default: return "EUR";
        }
    }
}
