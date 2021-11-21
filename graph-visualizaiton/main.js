var dataset = [],
i = 0;

for(i=0; i<5; i++){
    dataset.push({'x': Math.round(Math.random()*100)*3 + 50,
    'y': Math.round(Math.random()*100)*2 + 70}
    );
}

const width = $(window).innerWidth();
const height = $(window).innerHeight();

var sampleSVG = d3.select("body")
    .append("svg")
    .attr("width", width)
    .attr("height", height)

sampleSVG.selectAll("circle")
    .data(dataset)
    .enter()
    .append("circle")
    .style("stroke", "gray")
    .style("fill", "black")
    .attr("r", 30)
    .attr("cx", function(d){return d.x})
    .attr("cy", function(d){return d.y})
    .on("mouseover", function (d) {d3.select(this).style("cursor", "move");})
    .on("mouseout", function (d) {})
    .call(d3.drag()
            .on("start", dragstarted)
            .on("drag", dragged)
            .on("end", dragended)
            );

function dragstarted(event, d) {
    d3.select(this).raise().classed("active", true);
    }
function dragged(event, d) {
    d3.select(this).attr("cx", d.x = event.x).attr("cy", d.y = event.y);
    }
function dragended(event, d) {
    d3.select(this).classed("active", false);
    }

