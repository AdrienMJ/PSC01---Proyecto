package com.mycompany.app.service;

import com.mycompany.app.dto.BalancePersonaDTO;
import com.mycompany.app.dto.ResumenGrupoDTO;
import com.mycompany.app.dto.TransferenciaDTO;
import com.mycompany.app.entity.Gasto;
import com.mycompany.app.entity.CategoriaGasto;
import com.mycompany.app.entity.Grupo;
import com.mycompany.app.entity.Moneda;
import com.mycompany.app.entity.Pago;
import com.mycompany.app.entity.Usuario;
import com.mycompany.app.repository.GastoRepository;
import com.mycompany.app.repository.GrupoRepository;
import com.mycompany.app.repository.UsuarioRepository;
import com.mycompany.app.repository.PagoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired
    private PagoRepository pagoRepository;

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
        // Cargar el gasto básico
        Gasto gasto = gastoRepository.findById(gastoId)
            .orElseThrow(() -> new Exception("Gasto no encontrado"));

        if (gasto.isPagado()) {
            throw new Exception("El gasto ya está marcado como pagado");
        }
        
        // Cargar el grupo con sus miembros por separado
        Long grupoId = gasto.getGrupo() != null ? gasto.getGrupo().getId() : null;
        if (grupoId == null) {
            throw new Exception("El gasto no tiene grupo asociado");
        }
        
        Grupo grupo = grupoRepository.findById(grupoId)
            .orElseThrow(() -> new Exception("Grupo no encontrado"));
        
        List<Usuario> miembros = grupo.getMiembros();
        if (miembros == null || miembros.isEmpty()) {
            throw new Exception("El grupo no tiene miembros");
        }

        if (miembros.stream().noneMatch(u -> u.getId().equals(usuarioId))) {
            throw new Exception("Usuario no pertenece al grupo");
        }

        if (gasto.getPagador() != null && gasto.getPagador().getId().equals(usuarioId)) {
            throw new Exception("No puedes marcar tu propio gasto como pagado. Otro miembro del grupo debe confirmarlo.");
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

        // Aplicar pagos realizados al balance
        List<Pago> pagos = pagoRepository.findByGrupoId(grupoId);
        for (Pago pago : pagos) {
            if (pago.getMonto() != null && pago.getMonto() > 0
                    && pago.getPagador() != null && pago.getReceptor() != null) {
                Long pagadorId = pago.getPagador().getId();
                Long receptorId = pago.getReceptor().getId();
                if (balances.containsKey(pagadorId)) {
                    balances.put(pagadorId, balances.get(pagadorId) + pago.getMonto());
                }
                if (balances.containsKey(receptorId)) {
                    balances.put(receptorId, balances.get(receptorId) - pago.getMonto());
                }
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

    public Map<String, Object> obtenerTodasLasTasas(Moneda base) {
    try {
        String isoBase = obtenerCodigoIso(base);
        String url = "https://open.er-api.com/v6/latest/" + isoBase;
        RestTemplate restTemplate = new RestTemplate();
        
        Map<?, ?> response = restTemplate.getForObject(url, Map.class);
        if (response != null && response.containsKey("rates")) {
            Object ratesObj = response.get("rates");
            if (ratesObj instanceof Map<?, ?> rates) {
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<?, ?> entry : rates.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        result.put(key, entry.getValue());
                    }
                }
                return result;
            }
        }
        } catch (Exception e) {
            System.err.println("Error obteniendo tasas: " + e.getMessage());
        }
        return new HashMap<>();
    }

    public List<Map<String, Object>> obtenerResumenPorGrupoParaUsuario(Long userId) {
        usuarioRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Grupo> grupos = grupoRepository.findAll().stream()
                .filter(g -> g.getMiembros().stream().anyMatch(m -> m.getId().equals(userId)))
                .toList();

        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Grupo grupo : grupos) {
            List<Gasto> gastosDelGrupo = gastoRepository.findByGrupoId(grupo.getId());

            double totalPagadoPorUsuario = gastosDelGrupo.stream()
                    .filter(g -> g.getPagador() != null && g.getPagador().getId().equals(userId))
                    .filter(g -> g.getMonto() != null && g.getMonto() > 0)
                    .mapToDouble(Gasto::getMonto)
                    .sum();

            double totalParteUsuario = 0.0;
            for (Gasto gasto : gastosDelGrupo) {
                if (gasto.getMonto() == null || gasto.getMonto() <= 0) continue;

                List<Usuario> involucrados = (gasto.isRepartoGeneral() || gasto.getParticipantes() == null || gasto.getParticipantes().isEmpty())
                        ? grupo.getMiembros()
                        : gasto.getParticipantes();

                boolean participa = involucrados.stream().anyMatch(u -> u.getId().equals(userId));
                if (participa) {
                    totalParteUsuario += gasto.getMonto() / involucrados.size();
                }
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("grupoId", grupo.getId());
            entry.put("grupoNombre", grupo.getNombre());
            entry.put("moneda", grupo.getMoneda().name());
            entry.put("totalPagado", redondear2(totalPagadoPorUsuario));
            entry.put("totalParte", redondear2(totalParteUsuario));
            entry.put("numGastos", gastosDelGrupo.size());
            resultado.add(entry);
        }

        return resultado;
    }

    /**
     * Eliminar un gasto - solo el administrador del grupo puede hacerlo
     */
    @Transactional
    public void eliminarGasto(Long gastoId, Long usuarioId) throws Exception {
        // Cargar el gasto
        Gasto gasto = gastoRepository.findById(gastoId)
            .orElseThrow(() -> new Exception("Gasto no encontrado"));

        // Cargar el grupo por separado para evitar problemas de sesión
        Long grupoId = gasto.getGrupo().getId();
        Grupo grupo = grupoRepository.findById(grupoId)
            .orElseThrow(() -> new Exception("Grupo no encontrado"));

        // Verificar que el usuario es el administrador (creador) del grupo
        if (grupo.getIdCreador() == null || !grupo.getIdCreador().equals(usuarioId)) {
            throw new Exception("Solo el administrador del grupo puede eliminar gastos");
        }

        // Limpiar participantes primero para que Hibernate borre la tabla de unión antes que el gasto
        gasto.getParticipantes().clear();
        gastoRepository.delete(gasto);
    }

    /**
     * Editar un gasto - solo el administrador del grupo puede hacerlo
     * Permite editar: concepto, monto, categoría, participantes
     */
    public Gasto editarGasto(Long gastoId, Long usuarioId, Gasto gastoActualizado) throws Exception {
        // Cargar el gasto existente
        Gasto gasto = gastoRepository.findById(gastoId)
            .orElseThrow(() -> new Exception("Gasto no encontrado"));

        // Cargar el grupo por separado
        Long grupoId = gasto.getGrupo().getId();
        Grupo grupo = grupoRepository.findById(grupoId)
            .orElseThrow(() -> new Exception("Grupo no encontrado"));

        // Verificar que el usuario es el administrador (creador) del grupo
        if (grupo.getIdCreador() == null || !grupo.getIdCreador().equals(usuarioId)) {
            throw new Exception("Solo el administrador del grupo puede editar gastos");
        }

        // Actualizar campos permitidos
        if (gastoActualizado.getConcepto() != null) {
            gasto.setConcepto(gastoActualizado.getConcepto());
        }

        if (gastoActualizado.getMonto() != null && gastoActualizado.getMonto() > 0) {
            gasto.setMonto(gastoActualizado.getMonto());
        }

        if (gastoActualizado.getCategoria() != null) {
            gasto.setCategoria(gastoActualizado.getCategoria());
        }

        if (gastoActualizado.getEmote() != null) {
            gasto.setEmote(gastoActualizado.getEmote().isBlank() ? null : gastoActualizado.getEmote());
        }

        // Actualizar participantes si se proporcionan
        if (gastoActualizado.getParticipantes() != null && !gastoActualizado.getParticipantes().isEmpty()) {
            List<Usuario> participantesNuevos = gastoActualizado.getParticipantes();
            Set<Long> idsMiembros = grupo.getMiembros().stream()
                    .map(Usuario::getId)
                    .collect(Collectors.toSet());

            Set<Long> idsParticipantes = participantesNuevos.stream()
                    .map(Usuario::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(HashSet::new));

            if (!idsMiembros.containsAll(idsParticipantes)) {
                throw new Exception("Todos los participantes deben pertenecer al grupo");
            }

            List<Usuario> participantesFinales = new ArrayList<>(usuarioRepository.findAllById(idsParticipantes));
            gasto.setParticipantes(participantesFinales);
            gasto.setRepartoGeneral(participantesFinales.size() == grupo.getMiembros().size());
        }

        return gastoRepository.save(gasto);
    }
}
