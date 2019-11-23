import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './Site.css';
import MessagesLoader from "./MessagesLoader";
import Calendar from 'react-calendar';
import SiteTable from "./SiteTable";
import SiteRepo from "./SiteRepo";

class Site extends Component {
  constructor(props) {
    super(props);
    this.state = {
      siteName: '',
      messages: MessagesLoader.Empty,
      page: 0,
      pageSize: 10,
      orderBy: 'site.created_at desc',
      records: [],
      date: new Date(),
      time: "00:00"
    };
  }
  
  onDateChanged = date => this.setState({date});

  renderRecords = () => {
    SiteRepo.listToUpdate(
      this.state.page, this.state.pageSize, this.state.orderBy,
      (table) => {
        this.setState({
          records: table
        });
      },
      () => {
        this.props.history.push("/login/site");
      },
      (msg) => {
        console.log(msg);
      }
    );
  }

  componentDidMount = async() => {
    try {
      const resp = await fetch("/api/timeZoneInfo");

      if (resp.status === 200) {
        const json = await resp.json();
        const sel = document.getElementById('timeZoneSelect');
        const table = json['table'];
        for (let i = 0; i < table.length; ++i) {
          let opt = document.createElement('option');
          opt.value = i;
          opt.innerHTML = table[i];
          sel.appendChild(opt);
        }
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }

    this.renderRecords();

    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'siteName'},
          { key: 'register'},
          { key: 'timeZone'},
          { key: 'dateTime'},
          { key: 'siteMaintenance'},
          { key: 'error.unknown'},
          { key: 'duplicated'},
          { key: 'owner'},
          { key: 'recordEmpty'},
          { key: 'deleteConfirm'},
          { key: 'delete'},
          { key: 'cancel'},
          { key: 'date'},
          { key: 'time'},
          { key: 'update'},
          { key: 'deleted'},
          { key: 'agentRecordWillBeDeleted'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  onCreateSite = () => {
    this.createOrUpdateSite(
      "/api/createSite",
      () => {
        this.setState({
          selectedSite: undefined
        });
      }
    );
  }

  onUpdateSite = () => {
    this.createOrUpdateSite(
      "/api/updateSite?siteId=" + this.state.selectedSite.siteId,
      () => {
        this.setState({
          selectedSite: undefined
        });
      }
    );
  }

  onSiteDeleted = (siteId) => {
    this.renderRecords();
  }

  createOrUpdateSite = async(url, onSuccess) => {
    try {
      this.setState({
        siteNameError: undefined,
        dateTimeError: undefined,
        globalError: undefined,
        message: undefined
      });

      const dateTime =
            this.state.date.getFullYear() + "/" +
            ('0' + (this.state.date.getMonth() + 1)).slice(-2) + "/" +
            ('0' + this.state.date.getDate()).slice(-2) +
             ' ' + this.state.time;

      const resp = await fetch(
        url, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck"
          },
          body: JSON.stringify({
            siteName: this.state.siteName,
            dateTime: dateTime,
            timeZoneIndex: document.getElementById('timeZoneSelect').selectedIndex,
          })
        }
      );

      console.log("status: " + resp.status);
      if (resp.status === 200) {
        this.renderRecords();
        if (onSuccess !== undefined) onSuccess();
      } else if (resp.status === 400) {
        const json = await resp.json();
        console.log(JSON.stringify(json));
        this.setState({
          globalError: json[''],
          siteNameError: json['siteName'],
          dateTimeError: json['dateTime']
        });
      } else if (resp.status === 401) {
        this.props.history.push("/login/admin")
      } else if (resp.status === 403) {
        this.setState({
          globalError: this.msg('error.unknown')
        });
      } else if (resp.status === 404) {
        this.setState({
          globalError: this.msg('deleted')
        });
      } else if (resp.status === 409) {
        this.setState({
          globalError: [this.msg('duplicated')]
        });
      } else {
        this.setState({
          globalError: this.msg('error.unknown')
        });
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  onSiteSelected = (siteId, siteName, date, time, timezone) => {
    this.setState({
      selectedSite: {
        siteId: siteId
      },
      siteName: siteName,
      date,
      time
    });

    const sel = document.getElementById('timeZoneSelect');
    const opts = sel.children;
    for (let i = 0; i < opts.length; ++i) {
      if (opts[i].innerHTML === timezone) {
        sel.value = i;
        break;
      }
    }
  }

  render() {
    const siteNameError = this.state.siteNameError !== undefined ?
      <div className="error">
        { this.state.siteNameError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    const dateTimeError = this.state.dateTimeError !== undefined ?
      <div className="error">
        { this.state.dateTimeError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";


    return (
      <div className="site">
        { this.state.message !== undefined &&
          <article className="message is-info">
            <div className="message-body">{this.state.message}</div>
          </article>
        }

        { this.state.globalError !== undefined && 
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>
        }

        <nav className="panel">
          <p className="panel-heading">
            {this.msg('siteMaintenance')}
          </p>
          <div className="panel-block">
            <form>
              <div className="field">
                <div className="control siteName">
                  <label className="label">{this.msg('siteName')}</label>
                  <div className="control">
                    <input id="siteName" className="input" type="text" placeholder={this.msg('siteName')}
                           value={this.state.siteName} onChange={(e) => this.setState({siteName: e.target.value})}
                    />
                  </div>
                  { siteNameError }
                </div>
              </div>

              <div className="field">
                <div className="control date">
                  <label className="label">{this.msg('date')}</label>
                  <div className="control">
                    <Calendar id="openDate" onChange={this.onDateChanged} value={this.state.date}/>
                  </div>
                  { dateTimeError }
                </div>
              </div>

              <div className="field">
                <div className="control time">
                  <label className="label">{this.msg('time')}</label>
                  <div className="control">
                    <input id="openTime" className="input" type="text" value={this.state.time}
                           onChange={(e) => this.setState({time: e.target.value})}/>
                  </div>
                  { dateTimeError }
                </div>
              </div>

              <div className="field">
                <div className="control timeZone">
                  <label className="label">{this.msg('timeZone')}</label>
                  <div className="select">
                    <select id="timeZoneSelect" name="timeZoneSelect">
                    </select>
                  </div>
                </div>
              </div>

              <div className="field">
                <p className="control">
                  { this.state.selectedSite !== undefined ?
                    <button id="updateSite" type="button" className="button is-success update"
                            onClick={this.onUpdateSite}>{this.msg("update")}</button>
                    : ""}
                  <button id="createSite" type="button" className="button is-success"
                          onClick={this.onCreateSite}>{this.msg("register")}</button>
                </p>
              </div>
            </form>
          </div>
          <div className="panel-block">
            <SiteTable records={this.state.records} onSiteSelected={this.onSiteSelected} onSiteDeleted={this.onSiteDeleted}
                       canDeleteSite={true} selectedSite={this.state.selectedSite}/>
          </div>
        </nav>
      </div>
    );
  }
}

export default withRouter(Site);
