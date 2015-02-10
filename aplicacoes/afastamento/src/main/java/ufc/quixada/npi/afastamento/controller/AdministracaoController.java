package ufc.quixada.npi.afastamento.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ufc.quixada.npi.afastamento.model.Periodo;
import ufc.quixada.npi.afastamento.model.Professor;
import ufc.quixada.npi.afastamento.model.Ranking;
import ufc.quixada.npi.afastamento.model.Reserva;
import ufc.quixada.npi.afastamento.model.StatusPeriodo;
import ufc.quixada.npi.afastamento.model.StatusReserva;
import ufc.quixada.npi.afastamento.service.PeriodoService;
import ufc.quixada.npi.afastamento.service.ProfessorService;
import ufc.quixada.npi.afastamento.service.RankingService;
import ufc.quixada.npi.afastamento.service.ReservaService;
import ufc.quixada.npi.afastamento.util.Constants;

@Controller
@RequestMapping("administracao")
public class AdministracaoController {
	
	@Inject
	private ProfessorService professorService;
	
	@Inject
	private RankingService rankingService;
	
	@Inject
	private PeriodoService periodoService;
	
	@Inject
	private ReservaService reservaService;
	
	@RequestMapping(value = "/professores", method = RequestMethod.GET)
	public String listarProfessores(Model model) {
		List<Professor> professores = professorService.findAtivos();
		model.addAttribute("professores", professores);
		return Constants.PAGINA_LISTAR_PROFESSORES;
	}
	
	@RequestMapping(value = "/reservas", method = RequestMethod.GET)
	public String getReservas(Model model) {
		Periodo periodo = periodoService.getUltimoPeriodoEncerrado();
		if(periodo != null) {
			periodo = periodoService.getPeriodoPosterior(periodo);
			if(periodo != null) {
				periodo = periodoService.getPeriodoPosterior(periodo);
				Ranking ranking = rankingService.getRanking(periodo);
				model.addAttribute("ranking", ranking);
			}
		}
		
		return Constants.PAGINA_GERENCIAR_RESERVAS;
	}
	
	@RequestMapping(value = "/atualizar-ranking", method = RequestMethod.POST)
	@CacheEvict(value = {"default", "reservasByProfessor", "periodo", "visualizarRanking", "ranking", "loadProfessor", "professores"}, allEntries = true)
	public String atualizarRanking(HttpServletRequest request, RedirectAttributes redirect) {
		String[] status = request.getParameterValues("status");
		for(String s : status) {
			String[] valor = s.split("-");
			Reserva reserva = reservaService.find(Reserva.class, Long.parseLong(valor[0]));
			StatusReserva statusReserva = StatusReserva.valueOf(valor[1]);
			reserva.setStatus(statusReserva);
			reservaService.update(reserva);
		}
		
		redirect.addFlashAttribute(Constants.INFO, Constants.MSG_RESERVAS_ATUALIZADAS);
		return Constants.REDIRECT_PAGINA_GERENCIAR_RESERVAS;
	}

	@RequestMapping(value = "/periodos.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody List<Periodo> periodos() {
		return periodoService.getAll();
	}
	
	@RequestMapping(value = "/periodo", method = RequestMethod.GET)
	public String listarPeriodos(Model model) {
		model.addAttribute("periodo", new Periodo());
		return Constants.PAGINA_LISTAR_PERIODOS;
	}

	@RequestMapping(value = "/periodo", method = RequestMethod.POST)
	public String listarPeriodos(Model model, @RequestParam("ano") Integer ano, @RequestParam("semestre") Integer semestre) {
		Periodo periodo = periodoService.getPeriodo(ano, semestre);
		
		if(!notNull(periodo)){
			model.addAttribute("message", "Período " + ano + "." + semestre + " não está cadastrado.");
			return Constants.PAGINA_LISTAR_PERIODOS;
		} else if (periodo.getStatus().equals(StatusPeriodo.ENCERRADO)) {
			model.addAttribute("permitirUpdate", false);
			model.addAttribute("periodo", periodo);
			return Constants.PAGINA_LISTAR_PERIODOS;
		}

		boolean permitirUpdateEncerramento = false;
		boolean permitirUpdateVagas = false;
		
		Periodo periodoSolicitacao = periodoService.getPeriodo(periodo.getAno()-1, periodo.getSemestre());	

		if(periodo.getStatus().equals(StatusPeriodo.ABERTO)){
			permitirUpdateEncerramento = true;
		}

		if(periodoSolicitacao.getStatus().equals(StatusPeriodo.ABERTO)){
			permitirUpdateVagas = true;
		}
		
		model.addAttribute("permitirUpdateVagas", 		 permitirUpdateVagas);
		model.addAttribute("permitirUpdateEncerramento", permitirUpdateEncerramento);
		model.addAttribute("periodo", periodo);
		return Constants.PAGINA_LISTAR_PERIODOS;
	}
	
	private boolean notNull(Object object){
		return object != null ?  true : false;
	}
	
	@RequestMapping(value = "/update-periodo", method = RequestMethod.POST)
	@CacheEvict(value = {"default", "reservasByProfessor", "periodo", "visualizarRanking", "ranking", "loadProfessor", "professores"}, allEntries = true)
	public String atualizarPeriodo(Model model, RedirectAttributes redirectAttributes, @Valid @ModelAttribute("periodo") Periodo periodo, BindingResult result) {

		Date encerramento = periodo.getEncerramento();
		Integer vagas = periodo.getVagas();
		if(encerramento != null) {
			try {
				SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
				Date today;
					today = format.parse(format.format(new Date()));
				
				if(encerramento.before(today)) {
					model.addAttribute("periodo", periodo);
					model.addAttribute("errorData", Constants.MSG_DATA_FUTURA);
					return Constants.PAGINA_LISTAR_PERIODOS;
				}
			} catch (ParseException e) {
				model.addAttribute("periodo", periodo);
				model.addAttribute(Constants.ERRO, Constants.MSG_ERRO_ATUALIZAR_PERIODO);
				return Constants.PAGINA_LISTAR_PERIODOS;
			}
		}
		
		if(vagas == null) {
			vagas = 0;
		}
		
		boolean permitirUpdateEncerramento = false;
		boolean permitirUpdateVagas = false;

		periodo = periodoService.find(Periodo.class, periodo.getId());

		if(periodo.getStatus().equals(StatusPeriodo.ABERTO)){
			permitirUpdateEncerramento = true;
		}

		Periodo periodoSolicitacao = periodoService.getPeriodo(periodo.getAno() - 1, periodo.getSemestre());	
		if(periodoSolicitacao.getStatus().equals(StatusPeriodo.ABERTO)){
			permitirUpdateVagas = true;
		}
				
		model.addAttribute("permitirUpdateVagas", permitirUpdateVagas);
		model.addAttribute("permitirUpdateEncerramento", permitirUpdateEncerramento);

		if(permitirUpdateEncerramento){
			periodo.setEncerramento(encerramento);
		}

		if(permitirUpdateVagas){
			periodo.setVagas(vagas);
		}
			
		if(permitirUpdateEncerramento || permitirUpdateVagas){
			periodoService.update(periodo);
			model.addAttribute("periodo", periodo);
			model.addAttribute(Constants.INFO,"Período " +periodo.getAno() + "." + periodo.getSemestre() + " atualizado com sucesso.");
		}
		
		return Constants.PAGINA_LISTAR_PERIODOS;
	}
	
	@RequestMapping(value = "/admissao", method = RequestMethod.POST)
	@CacheEvict(value = {"default", "reservasByProfessor", "periodo", "visualizarRanking", "ranking", "loadProfessor", "professores"}, allEntries = true)
	public String atualizaAdmissao(@RequestParam("id") Long id, @RequestParam("ano") Integer ano, @RequestParam("semestre") Integer semestre, Model model) {

		if (id == null || ano == null || semestre == null) {
			model.addAttribute(Constants.ERRO, "Dados inválidos");
			return Constants.PAGINA_LISTAR_PROFESSORES;
		}
		
		Professor professor = professorService.find(Professor.class, id);
		professor.setAnoAdmissao(ano);
		professor.setSemestreAdmissao(semestre);

		professorService.update(professor);
		
		List<Professor> professors = professorService.findAtivos();
		model.addAttribute("professores", professors);
		model.addAttribute("info", "Data de admissão do(a) Prof(a) " + professor.getNome() + " atualizada com sucesso.");

		return Constants.PAGINA_LISTAR_PROFESSORES;
	}
	

}
