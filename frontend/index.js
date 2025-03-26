window.onload = function () {
    fetch("http://localhost:8080/api/trees/list")
        .then(response => response.json())
        .then(data => {
            const list = document.getElementById("tree-list");
            list.innerHTML = ""; // Clear list

            data.forEach(tree => {
                const li = document.createElement("li");
                li.innerHTML = `
                    <strong>${tree.name}</strong><br>
                    Location: ${tree.location}<br>
                    ${tree.imageUrl ? `<img src="${tree.imageUrl}" width="150">` : ""}
                `;
                list.appendChild(li);
            });
        })
        .catch(error => console.error("Error loading trees:", error));
};

