window.onload = function () {
    fetch("http://localhost:8080/api/trees/list")
        .then(response => response.json())
        .then(data => {
            const container = document.querySelector(".tree-list") || document.createElement("div");
            container.className = "tree-list";
            container.innerHTML = ""; // Clear old data

            data.forEach(tree => {
                const treeCard = document.createElement("div");
                treeCard.style.border = "1px solid #ccc";
                treeCard.style.margin = "10px";
                treeCard.style.padding = "10px";
                treeCard.style.borderRadius = "10px";
                treeCard.style.backgroundColor = "#f9f9f9";

                treeCard.innerHTML = `
                    <h3>${tree.name}</h3>
                    <p><strong>Location:</strong> ${tree.location}</p>
                    ${tree.imageUrl ? `<img src="${tree.imageUrl}" width="150">` : ""}
                `;

                container.appendChild(treeCard);
            });

            document.body.appendChild(container);
        })
        .catch(error => console.error("Error loading trees:", error));
};
