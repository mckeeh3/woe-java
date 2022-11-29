fetch('https://green-voice-3640.us-east1.kalix.app/generator/generator-2/create', {
  method: 'POST',
  headers: {
    'user-agent': 'vscode-restclient',
    'content-type': 'application/json',
  },
  body: {
    generatorId: 'generator-2',
    radiusKm: 100,
    generate: 1000,
    ratePerSecond: 1000,
  },
})
  .then((response) => {
    console.log(response);
  })
  .catch((err) => {
    console.error(err);
  });
