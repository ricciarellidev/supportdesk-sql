"use strict";
const $ = (id) => document.getElementById(id);
const labels = {
  OPEN: "Aberto",
  IN_PROGRESS: "Em andamento",
  RESOLVED: "Resolvido",
  CLOSED: "Encerrado",
  LOW: "Baixa",
  MEDIUM: "Média",
  HIGH: "Alta",
  CRITICAL: "Crítica",
};
let page = 0,
  totalPages = 0,
  loadSequence = 0;

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

async function api(path) {
  const response = await fetch("/api/v1" + path, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error("Não foi possível consultar os dados.");
  return response.json();
}

function tag(value) {
  return element("span", labels[value] || value, "tag " + value);
}
function date(value) {
  return value
    ? new Date(value).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" })
    : "—";
}
function decimal(value) {
  return Number(value).toLocaleString("pt-BR", { maximumFractionDigits: 2 });
}

function renderTickets(data) {
  $("tickets").replaceChildren();
  totalPages = data.totalPages;
  $("ticket-count").textContent = data.totalElements + " registros";
  $("page-label").textContent = data.totalElements
    ? "Página " + (data.page + 1) + " de " + totalPages
    : "Nenhum chamado encontrado";
  $("previous").disabled = page === 0;
  $("next").disabled = page + 1 >= totalPages;
  if (!data.content.length) {
    const row = element("tr");
    const cell = element("td", "Nenhum chamado corresponde aos filtros.", "empty");
    cell.colSpan = 5;
    row.append(cell);
    $("tickets").append(row);
  }
  for (const ticket of data.content) {
    const row = element("tr");
    const title = element("td");
    const button = element("button", undefined, "ticket-link");
    button.append(
      element("small", "#" + String(ticket.id).padStart(4, "0")),
      element("span", ticket.title),
    );
    button.addEventListener("click", () => showDetail(ticket.id));
    title.append(button);
    row.append(
      title,
      element("td", ticket.categoryName),
      element("td", ticket.assignedAgentName || "Sem responsável"),
    );
    const priority = element("td"),
      status = element("td");
    priority.append(tag(ticket.priority));
    status.append(tag(ticket.status));
    row.append(priority, status);
    $("tickets").append(row);
  }
}

function renderCategories(data) {
  $("categories").replaceChildren();
  const total = data.reduce((sum, item) => sum + item.totalTickets, 0);
  for (const category of data) {
    const row = element("div", undefined, "category-row"),
      meta = element("div", undefined, "category-meta");
    meta.append(
      element("span", category.categoryName),
      element("strong", String(category.totalTickets)),
    );
    const track = element("div", undefined, "track"),
      fill = element("div", undefined, "fill");
    fill.style.width = (total ? (category.totalTickets / total) * 100 : 0) + "%";
    track.setAttribute("aria-hidden", "true");
    track.append(fill);
    row.append(meta, track);
    $("categories").append(row);
  }
  if (!data.length) $("categories").append(element("p", "Nenhuma categoria cadastrada.", "empty"));
}

function renderAgents(data) {
  $("agents").replaceChildren();
  for (const agent of data) {
    const row = element("div", undefined, "agent-row");
    const initials = agent.agentName
      .split(" ")
      .filter(Boolean)
      .map((name) => name[0])
      .slice(0, 2)
      .join("");
    const name = element("div", agent.agentName, "agent-name");
    name.append(
      element("small", agent.openBacklog + " na fila · " + agent.resolvedTickets + " resolvidos"),
    );
    row.append(
      element("span", initials, "avatar"),
      name,
      element("span", String(agent.assignedTickets), "agent-total"),
    );
    $("agents").append(row);
  }
  if (!data.length) $("agents").append(element("p", "Nenhum agente ativo cadastrado.", "empty"));
}

async function load() {
  const sequence = ++loadSequence;
  $("refresh").disabled = true;
  $("notice").className = "notice";
  $("notice").textContent = "Atualizando dados do atendimento…";
  const query = new URLSearchParams({ page: String(page), size: "5" });
  if ($("query").value.trim()) query.set("q", $("query").value.trim());
  if ($("status").value) query.set("status", $("status").value);
  try {
    const [overview, tickets, categories, agents, sla] = await Promise.all([
      api("/reports/overview"),
      api("/tickets?" + query),
      api("/reports/by-category"),
      api("/reports/by-agent"),
      api("/reports/sla"),
    ]);
    if (sequence !== loadSequence) return;
    $("total").textContent = overview.totalTickets;
    $("backlog").textContent = overview.openBacklog;
    $("resolved").textContent = overview.resolvedTickets;
    const resolved = sla.reduce((sum, item) => sum + item.resolvedTickets, 0),
      within = sla.reduce((sum, item) => sum + item.withinSla, 0);
    $("sla").textContent = resolved ? decimal((within / resolved) * 100) + "%" : "—";
    $("overdue").textContent = overview.overdueBacklog + " chamados com prazo vencido";
    $("average").textContent =
      overview.averageResolutionHours === null
        ? "Sem resoluções para calcular a média"
        : decimal(overview.averageResolutionHours) + " horas para resolução";
    renderTickets(tickets);
    renderCategories(categories);
    renderAgents(agents);
    $("notice").textContent =
      "Dados carregados. O painel é de consulta; alterações estão disponíveis pela API.";
    $("updated").textContent =
      "Atualizado às " +
      new Date().toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  } catch (error) {
    if (sequence !== loadSequence) return;
    $("notice").className = "notice error";
    $("notice").textContent =
      "Não foi possível carregar o painel. Verifique se a aplicação está em execução e tente atualizar.";
  } finally {
    if (sequence === loadSequence) $("refresh").disabled = false;
  }
}

async function showDetail(id) {
  $("detail-title").textContent = "Carregando chamado…";
  $("detail-content").replaceChildren();
  $("detail").showModal();
  try {
    const data = await api("/tickets/" + id),
      ticket = data.ticket;
    $("detail-title").textContent = "#" + id + " · " + ticket.title;
    const description = element("p", ticket.description, "detail-description"),
      definition = element("dl");
    for (const [key, value] of [
      ["Solicitante", ticket.requesterName],
      ["Categoria", ticket.categoryName],
      ["Responsável", ticket.assignedAgentName || "Sem responsável"],
      ["Status", labels[ticket.status]],
      ["Prioridade", labels[ticket.priority]],
      ["Abertura", date(ticket.createdAt)],
      ["Prazo de resolução", date(ticket.dueAt)],
      ["Resolução", date(ticket.resolvedAt)],
    ]) {
      definition.append(element("dt", key), element("dd", value));
    }
    const history = element("ol", undefined, "history");
    for (const event of data.history) {
      const item = element("li", event.message);
      item.append(element("small", event.actorName + " · " + date(event.createdAt)));
      history.append(item);
    }
    $("detail-content").append(
      description,
      definition,
      element("h3", "Histórico do atendimento"),
      history,
    );
  } catch (error) {
    $("detail-title").textContent = "Chamado indisponível";
    $("detail-content").append(element("p", "Não foi possível consultar este chamado."));
  }
}

$("refresh").addEventListener("click", load);
$("filters").addEventListener("submit", (event) => {
  event.preventDefault();
  page = 0;
  load();
});
$("previous").addEventListener("click", () => {
  if (page > 0) {
    page--;
    load();
  }
});
$("next").addEventListener("click", () => {
  if (page + 1 < totalPages) {
    page++;
    load();
  }
});
$("close-detail").addEventListener("click", () => $("detail").close());
load();
